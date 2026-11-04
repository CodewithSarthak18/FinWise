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
public class CategoryComparisonItem {

    private String categoryName;
    private BigDecimal currentAmount;
    private BigDecimal previousAmount;
    private BigDecimal difference;
    private double percentageChange;
}
