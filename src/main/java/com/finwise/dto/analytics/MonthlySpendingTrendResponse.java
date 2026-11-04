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
public class MonthlySpendingTrendResponse {

    private String monthYear;
    private String label;
    private BigDecimal totalSpending;
    private BigDecimal dailyAverage;
    private long transactionCount;
    private String highestExpenseCategory;
    private BigDecimal highestExpenseAmount;
}
