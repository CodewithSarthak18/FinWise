package com.finwise.controller;

import com.finwise.dto.recurring.RecurringTransactionResponse;
import com.finwise.dto.response.ApiResponse;
import com.finwise.dto.subscription.ConfirmSubscriptionRequest;
import com.finwise.dto.subscription.SubscriptionDetectionSummaryResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.SubscriptionDetectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionDetectorController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDetectorController.class);
    private final SubscriptionDetectorService subscriptionDetectorService;

    @GetMapping("/detected")
    public ResponseEntity<ApiResponse<SubscriptionDetectionSummaryResponse>> getDetectedSubscriptions(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Detecting subscriptions for user id {} over past {} months", principal.getId(), months);
        SubscriptionDetectionSummaryResponse summary = subscriptionDetectorService.detectSubscriptions(principal.getId(), months);
        return ResponseEntity.ok(ApiResponse.success("Subscriptions detected successfully", summary));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> confirmDetectedSubscription(
            @Valid @RequestBody ConfirmSubscriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Confirming detected subscription '{}' of ₹{} for user id {}",
                request.getSubscriptionName(), request.getAmount(), principal.getId());
        RecurringTransactionResponse response = subscriptionDetectorService.confirmDetectedSubscription(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription confirmed and added to recurring payments", response));
    }

    @PostMapping("/{candidateId}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismissDetectedSubscription(
            @PathVariable String candidateId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Dismissing subscription suggestion {} for user id {}", candidateId, principal.getId());
        subscriptionDetectorService.dismissDetectedSubscription(principal.getId(), candidateId);
        return ResponseEntity.ok(ApiResponse.success("Subscription suggestion dismissed", null));
    }
}
