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
public class IncomeExpenseReportData {
    private ReportMetadata metadata;
    private BigDecimal totalIncome;
    private String formattedTotalIncome;
    private BigDecimal totalExpenses;
    private String formattedTotalExpenses;
    private BigDecimal netCashFlow;
    private String formattedNetCashFlow;
    private double savingsRate;
    private String formattedSavingsRate;

    // 50/30/20 Breakdown
    private BigDecimal needsAmount;
    private String formattedNeedsAmount;
    private double needsPercentage;
    private String formattedNeedsPercentage;

    private BigDecimal wantsAmount;
    private String formattedWantsAmount;
    private double wantsPercentage;
    private String formattedWantsPercentage;

    private BigDecimal savingsInvestmentsAmount;
    private String formattedSavingsInvestmentsAmount;
    private double savingsInvestmentsPercentage;
    private String formattedSavingsInvestmentsPercentage;

    @Builder.Default
    private List<ReportCategorySummary> incomeCategories = new ArrayList<>();

    @Builder.Default
    private List<ReportCategorySummary> expenseCategories = new ArrayList<>();

    @Builder.Default
    private List<ReportTransactionItem> transactions = new ArrayList<>();
}
