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
public class MonthlyReportData {
    private ReportMetadata metadata;
    private String period; // e.g. "November 2026"
    private BigDecimal totalIncome;
    private String formattedTotalIncome;
    private BigDecimal totalExpenses;
    private String formattedTotalExpenses;
    private BigDecimal netSavings;
    private String formattedNetSavings;
    private double savingsRate;
    private String formattedSavingsRate;
    private BigDecimal netWorth;
    private String formattedNetWorth;
    private long transactionCount;

    // Budget summary
    private boolean hasBudget;
    private String budgetName;
    private BigDecimal budgetAmount;
    private String formattedBudgetAmount;
    private BigDecimal budgetSpent;
    private String formattedBudgetSpent;
    private BigDecimal budgetRemaining;
    private String formattedBudgetRemaining;
    private double budgetUtilization;
    private String formattedBudgetUtilization;
    private String budgetStatus;

    @Builder.Default
    private List<ReportCategorySummary> categoryBreakdown = new ArrayList<>();

    @Builder.Default
    private List<ReportTransactionItem> transactions = new ArrayList<>();
}
