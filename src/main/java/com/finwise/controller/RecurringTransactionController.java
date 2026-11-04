package com.finwise.controller;

import com.finwise.dto.recurring.RecurringTransactionRequest;
import com.finwise.dto.recurring.RecurringTransactionResponse;
import com.finwise.dto.recurring.UpcomingReminderResponse;
import com.finwise.dto.response.ApiResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recurring")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionController.class);
    private final RecurringTransactionService recurringService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getRecurringTransactions(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching recurring transactions for user id: {}", principal.getId());
        List<RecurringTransactionResponse> responses = recurringService.getRecurringTransactions(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> getRecurringTransactionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching recurring transaction id: {} for user id: {}", id, principal.getId());
        RecurringTransactionResponse response = recurringService.getRecurringTransactionById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> createRecurringTransaction(
            @Valid @RequestBody RecurringTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Creating recurring transaction '{}' of ₹{} for user id: {}",
                request.getTitle(), request.getAmount(), principal.getId());
        RecurringTransactionResponse response = recurringService.createRecurringTransaction(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recurring transaction created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> updateRecurringTransaction(
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Updating recurring transaction id: {} for user id: {}", id, principal.getId());
        RecurringTransactionResponse response = recurringService.updateRecurringTransaction(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecurringTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Deleting recurring transaction id: {} for user id: {}", id, principal.getId());
        recurringService.deleteRecurringTransaction(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction deleted successfully", null));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> pauseRecurringTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Pausing recurring transaction id: {} for user id: {}", id, principal.getId());
        RecurringTransactionResponse response = recurringService.pauseRecurringTransaction(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction paused successfully", response));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> resumeRecurringTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Resuming recurring transaction id: {} for user id: {}", id, principal.getId());
        RecurringTransactionResponse response = recurringService.resumeRecurringTransaction(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction resumed successfully", response));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<UpcomingReminderResponse>>> getUpcomingReminders(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching upcoming reminders for next {} days for user id: {}", days, principal.getId());
        List<UpcomingReminderResponse> reminders = recurringService.getUpcomingRecurring(principal.getId(), days);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> executeRecurringTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Manually triggering recurring transaction id: {} for user id: {}", id, principal.getId());
        RecurringTransactionResponse response = recurringService.executeRecurringTransaction(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction executed successfully", response));
    }

    @PostMapping("/process-due")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processDueRecurringTransactions() {
        log.info("Processing due recurring transactions triggered via API");
        int processedCount = recurringService.processDueRecurringTransactions();
        return ResponseEntity.ok(ApiResponse.success(
                "Due recurring transactions processed",
                Map.of("processedCount", processedCount)
        ));
    }
}
