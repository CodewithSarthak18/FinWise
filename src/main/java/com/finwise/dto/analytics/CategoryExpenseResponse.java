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
public class CategoryExpenseResponse {

    private Long categoryId;
    private String categoryName;
    private String colorCode;
    private String icon;
    private BigDecimal amount;
    private double percentage;
    private long transactionCount;
}
