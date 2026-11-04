package com.finwise.controller;

import com.finwise.dto.goal.GoalContributionRequest;
import com.finwise.dto.goal.GoalContributionResponse;
import com.finwise.dto.goal.GoalRequest;
import com.finwise.dto.goal.GoalResponse;
import com.finwise.dto.response.ApiResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private static final Logger log = LoggerFactory.getLogger(GoalController.class);
    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoals(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching all goals for user id: {}", principal.getId());
        List<GoalResponse> response = goalService.getGoals(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching goal id: {} for user id: {}", id, principal.getId());
        GoalResponse response = goalService.getGoalById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(
            @Valid @RequestBody GoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Creating goal '{}' of ₹{} for user id: {}",
                request.getTitle(), request.getTargetAmount(), principal.getId());
        GoalResponse response = goalService.createGoal(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Savings goal created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Updating goal id: {} for user id: {}", id, principal.getId());
        GoalResponse response = goalService.updateGoal(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Savings goal updated successfully", response));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<ApiResponse<GoalResponse>> contributeToGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalContributionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Contributing ₹{} to goal id: {} from account id: {} for user id: {}",
                request.getAmount(), id, request.getAccountId(), principal.getId());
        GoalResponse response = goalService.contributeToGoal(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Contribution recorded successfully", response));
    }

    @GetMapping("/{id}/contributions")
    public ResponseEntity<ApiResponse<List<GoalContributionResponse>>> getGoalContributions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching contributions for goal id: {} for user id: {}", id, principal.getId());
        List<GoalContributionResponse> response = goalService.getGoalContributions(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Deleting goal id: {} for user id: {}", id, principal.getId());
        goalService.deleteGoal(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Goal deleted successfully", null));
    }
}
