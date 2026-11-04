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
public class AnnualReportData {
    private ReportMetadata metadata;
    private int year;
    private BigDecimal totalIncome;
    private String formattedTotalIncome;
    private BigDecimal totalExpenses;
    private String formattedTotalExpenses;
    private BigDecimal netSavings;
    private String formattedNetSavings;
    private double savingsRate;
    private String formattedSavingsRate;
    private BigDecimal monthlyAverageExpense;
    private String formattedMonthlyAverageExpense;
    private BigDecimal monthlyAverageIncome;
    private String formattedMonthlyAverageIncome;

    @Builder.Default
    private List<AnnualMonthlyRow> monthlyBreakdown = new ArrayList<>();

    @Builder.Default
    private List<ReportCategorySummary> topExpenseCategories = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnualMonthlyRow {
        private String monthName;
        private int monthNumber;
        private BigDecimal income;
        private String formattedIncome;
        private BigDecimal expenses;
        private String formattedExpenses;
        private BigDecimal netSavings;
        private String formattedNetSavings;
        private double savingsRate;
        private String formattedSavingsRate;
    }
}
