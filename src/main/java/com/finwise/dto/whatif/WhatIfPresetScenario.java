package com.finwise.dto.whatif;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfPresetScenario {

    private BigDecimal percentage;
    private String label;
    private BigDecimal reducedMonthlySpending;
    private BigDecimal monthlySavings;
    private BigDecimal estimatedYearlySavings;
}
