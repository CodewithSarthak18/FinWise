package com.finwise.dto.whatif;

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
public class WhatIfSimulationResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal reductionPercentage;
    private BigDecimal currentMonthlySpending;
    private BigDecimal reducedMonthlySpending;
    private BigDecimal monthlySavings;
    private BigDecimal estimatedYearlySavings;
    private String currencyCode;
    private String summary;
    private List<WhatIfPresetScenario> scenarios;
    private String notice; // "Simulated projection only. Real transaction records and account balances have NOT been modified."
}
