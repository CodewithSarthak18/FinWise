package com.finwise.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPerformanceReportData {
    private ReportMetadata metadata;
    private Long budgetId;
    private String budgetName;
    private String periodType;
    private String startDate;
    private String endDate;
    private BigDecimal totalBudgetAmount;
    private String formattedTotalBudgetAmount;
    private BigDecimal totalSpentAmount;
    private String formattedTotalSpentAmount;
    private BigDecimal remainingAmount;
    private String formattedRemainingAmount;
    private double utilizationPercentage;
    private String formattedUtilizationPercentage;
    private String alertStatus;
    private BigDecimal notificationThreshold;
    private boolean hasBudget;

    @Builder.Default
    private List<BudgetCategoryPerformanceItem> categoryPerformance = new ArrayList<>();

    @Builder.Default
    private List<String> observations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetCategoryPerformanceItem {
        private String categoryName;
        private BigDecimal allocatedAmount;
        private String formattedAllocatedAmount;
        private BigDecimal spentAmount;
        private String formattedSpentAmount;
        private BigDecimal remainingAmount;
        private String formattedRemainingAmount;
        private double utilizationPercentage;
        private String formattedUtilizationPercentage;
        private String status;
        private boolean overBudget;
    }
}
