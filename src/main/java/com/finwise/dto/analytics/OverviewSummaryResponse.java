package com.finwise.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewSummaryResponse {

    private String period; // e.g. "2026-11"
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private double savingsRate;
    private BigDecimal netWorth;
    private String currencyCode;
    private long transactionCount;
    private boolean hasActiveBudget;
    private double budgetUtilization;
}
