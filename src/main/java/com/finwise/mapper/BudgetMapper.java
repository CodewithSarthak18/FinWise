package com.finwise.mapper;

import com.finwise.dto.budget.BudgetCategoryResponse;
import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.budget.BudgetResponse;
import com.finwise.entity.Budget;
import com.finwise.entity.BudgetAlertStatus;
import com.finwise.entity.BudgetCategory;
import com.finwise.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BudgetMapper {

    public Budget toEntity(BudgetRequest request, User user) {
        if (request == null) {
            return null;
        }

        return Budget.builder()
                .user(user)
                .name(request.getName())
                .periodType(request.getPeriodType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalBudgetAmount(request.getTotalBudgetAmount())
                .notificationThreshold(request.getNotificationThreshold() != null ?
                        request.getNotificationThreshold() : new BigDecimal("80.00"))
                .active(true)
                .build();
    }

    public BudgetResponse toResponse(Budget budget, Map<Long, BigDecimal> categorySpentMap) {
        if (budget == null) {
            return null;
        }

        BigDecimal totalSpent = BigDecimal.ZERO;
        List<BudgetCategoryResponse> categoryResponses = Collections.emptyList();

        if (budget.getBudgetCategories() != null && !budget.getBudgetCategories().isEmpty()) {
            categoryResponses = budget.getBudgetCategories().stream().map(bc -> {
                Long catId = bc.getCategory().getId();
                BigDecimal spent = categorySpentMap.getOrDefault(catId, BigDecimal.ZERO);
                BigDecimal allocated = bc.getAllocatedAmount();
                BigDecimal remaining = allocated.subtract(spent);

                double util = 0.0;
                if (allocated.compareTo(BigDecimal.ZERO) > 0) {
                    util = spent.multiply(new BigDecimal("100"))
                            .divide(allocated, 2, RoundingMode.HALF_EVEN)
                            .doubleValue();
                }

                BudgetAlertStatus catAlert = determineAlertStatus(util);

                return BudgetCategoryResponse.builder()
                        .categoryId(catId)
                        .categoryName(bc.getCategory().getName())
                        .colorCode(bc.getCategory().getColorCode())
                        .icon(bc.getCategory().getIcon())
                        .allocatedAmount(allocated)
                        .spentAmount(spent)
                        .remainingAmount(remaining)
                        .utilizationPercentage(util)
                        .alertStatus(catAlert)
                        .build();
            }).collect(Collectors.toList());

            for (BudgetCategoryResponse catResp : categoryResponses) {
                totalSpent = totalSpent.add(catResp.getSpentAmount());
            }
        }

        BigDecimal totalBudget = budget.getTotalBudgetAmount();
        BigDecimal remainingBudget = totalBudget.subtract(totalSpent);

        double totalUtil = 0.0;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            totalUtil = totalSpent.multiply(new BigDecimal("100"))
                    .divide(totalBudget, 2, RoundingMode.HALF_EVEN)
                    .doubleValue();
        }

        BudgetAlertStatus overallAlert = determineAlertStatus(totalUtil);
        boolean exceeded = totalUtil >= budget.getNotificationThreshold().doubleValue();

        return BudgetResponse.builder()
                .id(budget.getId())
                .name(budget.getName())
                .periodType(budget.getPeriodType())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .totalBudgetAmount(totalBudget)
                .totalSpentAmount(totalSpent)
                .remainingAmount(remainingBudget)
                .utilizationPercentage(totalUtil)
                .alertStatus(overallAlert)
                .notificationThreshold(budget.getNotificationThreshold())
                .thresholdExceeded(exceeded)
                .active(budget.isActive())
                .categories(categoryResponses)
                .createdAt(budget.getCreatedAt())
                .build();
    }

    public static BudgetAlertStatus determineAlertStatus(double utilizationPercentage) {
        if (utilizationPercentage >= 120.0) {
            return BudgetAlertStatus.OVERSPENDING;
        } else if (utilizationPercentage >= 100.0) {
            return BudgetAlertStatus.EXCEEDED;
        } else if (utilizationPercentage >= 80.0) {
            return BudgetAlertStatus.WARNING;
        } else {
            return BudgetAlertStatus.NORMAL;
        }
    }
}
