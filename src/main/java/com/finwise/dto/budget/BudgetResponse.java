package com.finwise.dto.budget;

import com.finwise.entity.BudgetAlertStatus;
import com.finwise.entity.BudgetPeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private String name;
    private BudgetPeriodType periodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalBudgetAmount;
    private BigDecimal totalSpentAmount;
    private BigDecimal remainingAmount;
    private double utilizationPercentage;
    private BudgetAlertStatus alertStatus;
    private BigDecimal notificationThreshold;
    private boolean thresholdExceeded;
    private boolean active;
    private List<BudgetCategoryResponse> categories;
    private Instant createdAt;
}
