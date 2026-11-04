package com.finwise.service.impl;

import com.finwise.dto.budget.BudgetCategoryRequest;
import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.budget.BudgetResponse;
import com.finwise.entity.Budget;
import com.finwise.entity.BudgetCategory;
import com.finwise.entity.Category;
import com.finwise.entity.User;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.DuplicateResourceException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.exception.UnauthorizedException;
import com.finwise.mapper.BudgetMapper;
import com.finwise.repository.BudgetRepository;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.repository.UserRepository;
import com.finwise.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getActiveBudget(Long userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Budget budget = budgetRepository.findActiveBudgetForDate(userId, today)
                .orElse(null);

        if (budget == null) {
            return null;
        }

        Map<Long, BigDecimal> categorySpentMap = calculateCategorySpends(userId, budget);
        return budgetMapper.toResponse(budget, categorySpentMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Long userId) {
        return budgetRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(budget -> {
                    Map<Long, BigDecimal> categorySpentMap = calculateCategorySpends(userId, budget);
                    return budgetMapper.toResponse(budget, categorySpentMap);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", budgetId));

        Map<Long, BigDecimal> categorySpentMap = calculateCategorySpends(userId, budget);
        return budgetMapper.toResponse(budget, categorySpentMap);
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request, Long userId) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Budget end date cannot be before start date");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Prevent duplicate overlapping active budgets for the same period
        List<Budget> existingBudgets = budgetRepository.findByUserIdOrderByStartDateDesc(userId);
        for (Budget b : existingBudgets) {
            if (b.isActive() && datesOverlap(b.getStartDate(), b.getEndDate(), request.getStartDate(), request.getEndDate())) {
                throw new DuplicateResourceException(String.format(
                        "An active budget '%s' already exists for the overlapping period (%s to %s)",
                        b.getName(), b.getStartDate(), b.getEndDate()));
            }
        }

        // Validate no duplicate categories in request
        validateNoDuplicateCategories(request.getCategoryAllocations());

        Budget budget = budgetMapper.toEntity(request, user);

        if (request.getCategoryAllocations() != null) {
            for (BudgetCategoryRequest catReq : request.getCategoryAllocations()) {
                Category category = categoryRepository.findByIdAndDeletedAtIsNull(catReq.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", catReq.getCategoryId()));

                if (!category.isSystem() && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
                    throw new UnauthorizedException("You do not have access to category with id: " + catReq.getCategoryId());
                }

                BudgetCategory budgetCategory = BudgetCategory.builder()
                        .budget(budget)
                        .category(category)
                        .allocatedAmount(catReq.getAllocatedAmount())
                        .build();

                budget.addBudgetCategory(budgetCategory);
            }
        }

        Budget savedBudget = budgetRepository.save(budget);
        Map<Long, BigDecimal> categorySpentMap = calculateCategorySpends(userId, savedBudget);
        return budgetMapper.toResponse(savedBudget, categorySpentMap);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Long budgetId, BudgetRequest request, Long userId) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Budget end date cannot be before start date");
        }

        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", budgetId));

        // Validate no duplicate categories
        validateNoDuplicateCategories(request.getCategoryAllocations());

        budget.setName(request.getName());
        budget.setPeriodType(request.getPeriodType());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setTotalBudgetAmount(request.getTotalBudgetAmount());
        if (request.getNotificationThreshold() != null) {
            budget.setNotificationThreshold(request.getNotificationThreshold());
        }

        // Clear and rebuild category allocations
        budget.getBudgetCategories().clear();
        if (request.getCategoryAllocations() != null) {
            for (BudgetCategoryRequest catReq : request.getCategoryAllocations()) {
                Category category = categoryRepository.findByIdAndDeletedAtIsNull(catReq.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", catReq.getCategoryId()));

                if (!category.isSystem() && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
                    throw new UnauthorizedException("You do not have access to category with id: " + catReq.getCategoryId());
                }

                BudgetCategory budgetCategory = BudgetCategory.builder()
                        .budget(budget)
                        .category(category)
                        .allocatedAmount(catReq.getAllocatedAmount())
                        .build();

                budget.addBudgetCategory(budgetCategory);
            }
        }

        Budget saved = budgetRepository.save(budget);
        Map<Long, BigDecimal> categorySpentMap = calculateCategorySpends(userId, saved);
        return budgetMapper.toResponse(saved, categorySpentMap);
    }

    @Override
    @Transactional
    public void deleteBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", budgetId));
        budgetRepository.delete(budget);
    }

    private boolean datesOverlap(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        return !startA.isAfter(endB) && !startB.isAfter(endA);
    }

    private void validateNoDuplicateCategories(List<BudgetCategoryRequest> allocations) {
        if (allocations == null) return;
        Set<Long> seen = new HashSet<>();
        for (BudgetCategoryRequest req : allocations) {
            if (!seen.add(req.getCategoryId())) {
                throw new BadRequestException("Duplicate category ID in budget allocation: " + req.getCategoryId());
            }
        }
    }

    private Map<Long, BigDecimal> calculateCategorySpends(Long userId, Budget budget) {
        Map<Long, BigDecimal> map = new HashMap<>();
        Instant startInstant = budget.getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = budget.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        if (budget.getBudgetCategories() != null) {
            for (BudgetCategory bc : budget.getBudgetCategories()) {
                Long categoryId = bc.getCategory().getId();
                // Excludes transfers; factors in refunds for accurate net category spend
                BigDecimal spent = transactionRepository.sumExpenseByCategoryIdAndDateRange(
                        userId, categoryId, startInstant, endInstant);
                BigDecimal refund = transactionRepository.sumRefundByCategoryIdAndDateRange(
                        userId, categoryId, startInstant, endInstant);
                BigDecimal netSpent = (spent != null ? spent : BigDecimal.ZERO)
                        .subtract(refund != null ? refund : BigDecimal.ZERO)
                        .max(BigDecimal.ZERO);
                map.put(categoryId, netSpent);
            }
        }
        return map;
    }
}
