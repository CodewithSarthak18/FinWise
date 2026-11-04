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
public class IncomeVsExpenseResponse {

    private String monthYear;
    private String label;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal netSavings;
    private double savingsRate;
    private String status; // "SURPLUS", "DEFICIT", "BALANCED"
}
