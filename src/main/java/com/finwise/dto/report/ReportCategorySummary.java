package com.finwise.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCategorySummary {
    private String categoryName;
    private String type; // EXPENSE or INCOME
    private BigDecimal amount;
    private String formattedAmount;
    private long transactionCount;
    private double percentageOfTotal;
    private String formattedPercentage;
    private BigDecimal averageAmount;
    private String formattedAverage;
}
