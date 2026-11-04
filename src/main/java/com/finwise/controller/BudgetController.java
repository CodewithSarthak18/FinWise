package com.finwise.controller;

import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.budget.BudgetResponse;
import com.finwise.dto.response.ApiResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.BudgetService;
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
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private static final Logger log = LoggerFactory.getLogger(BudgetController.class);
    private final BudgetService budgetService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<BudgetResponse>> getCurrentBudget(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching current active budget for user id: {}", principal.getId());
        BudgetResponse response = budgetService.getActiveBudget(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching all budgets for user id: {}", principal.getId());
        List<BudgetResponse> response = budgetService.getBudgets(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.debug("Fetching budget id: {} for user id: {}", id, principal.getId());
        BudgetResponse response = budgetService.getBudgetById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Creating budget '{}' of ₹{} for user id: {}",
                request.getName(), request.getTotalBudgetAmount(), principal.getId());
        BudgetResponse response = budgetService.createBudget(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Updating budget id: {} for user id: {}", id, principal.getId());
        BudgetResponse response = budgetService.updateBudget(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Deleting budget id: {} for user id: {}", id, principal.getId());
        budgetService.deleteBudget(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully", null));
    }
}
