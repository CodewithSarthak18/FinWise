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
public class CategorySpendingOption {

    private Long categoryId;
    private String categoryName;
    private String colorCode;
    private String icon;
    private BigDecimal currentMonthlySpending;
    private double percentageOfTotalExpenses;
}
