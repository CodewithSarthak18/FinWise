package com.finwise.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeedsWantsResponse {

    private String period;
    private BigDecimal totalExpenses;
    private BigDecimal needsAmount;
    private double needsPercentage;
    private BigDecimal wantsAmount;
    private double wantsPercentage;
    private BigDecimal savingsOrInvestmentsAmount;
    private double savingsOrInvestmentsPercentage;
    private String benchmarkRule; // "50/30/20 Rule: 50% Needs, 30% Wants, 20% Savings"
    private String analysis;
    private List<CategoryExpenseResponse> topNeedsCategories;
    private List<CategoryExpenseResponse> topWantsCategories;
}
