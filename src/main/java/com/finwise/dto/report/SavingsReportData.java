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
public class SavingsReportData {
    private ReportMetadata metadata;
    private BigDecimal netSavingsInPeriod;
    private String formattedNetSavingsInPeriod;
    private double savingsRateInPeriod;
    private String formattedSavingsRateInPeriod;

    private BigDecimal totalGoalTargetAmount;
    private String formattedTotalGoalTargetAmount;
    private BigDecimal totalGoalCurrentAmount;
    private String formattedTotalGoalCurrentAmount;
    private BigDecimal totalGoalRemainingAmount;
    private String formattedTotalGoalRemainingAmount;
    private double overallGoalProgress;
    private String formattedOverallGoalProgress;

    @Builder.Default
    private List<SavingsGoalReportItem> goals = new ArrayList<>();

    @Builder.Default
    private List<GoalContributionReportItem> contributions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavingsGoalReportItem {
        private String title;
        private BigDecimal targetAmount;
        private String formattedTargetAmount;
        private BigDecimal currentAmount;
        private String formattedCurrentAmount;
        private BigDecimal remainingAmount;
        private String formattedRemainingAmount;
        private double progressPercentage;
        private String formattedProgressPercentage;
        private String targetDate;
        private String priority;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalContributionReportItem {
        private String contributionDate;
        private String goalTitle;
        private String accountName;
        private BigDecimal amount;
        private String formattedAmount;
        private String note;
    }
}
