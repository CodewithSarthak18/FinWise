package com.finwise.service;

import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.budget.BudgetResponse;

import java.util.List;

public interface BudgetService {

    BudgetResponse getActiveBudget(Long userId);

    List<BudgetResponse> getBudgets(Long userId);

    BudgetResponse getBudgetById(Long budgetId, Long userId);

    BudgetResponse createBudget(BudgetRequest request, Long userId);

    BudgetResponse updateBudget(Long budgetId, BudgetRequest request, Long userId);

    void deleteBudget(Long budgetId, Long userId);
}
