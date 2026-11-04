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
public class CashFlowResponse {

    private String monthYear; // e.g. "2026-11"
    private String label;     // e.g. "Nov 2026"
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal netSavings;
}
