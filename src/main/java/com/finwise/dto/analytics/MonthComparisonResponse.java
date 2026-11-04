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
public class MonthComparisonResponse {

    private String currentMonth;
    private String previousMonth;

    private BigDecimal currentIncome;
    private BigDecimal previousIncome;
    private BigDecimal incomeDifference;
    private double incomeChangePercentage;

    private BigDecimal currentExpense;
    private BigDecimal previousExpense;
    private BigDecimal expenseDifference;
    private double expenseChangePercentage;

    private BigDecimal currentSavings;
    private BigDecimal previousSavings;
    private BigDecimal savingsDifference;
    private double savingsChangePercentage;

    private double currentSavingsRate;
    private double previousSavingsRate;
    private double savingsRateDifference;

    private List<CategoryComparisonItem> topIncreasedCategories;
    private List<CategoryComparisonItem> topDecreasedCategories;
}
