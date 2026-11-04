package com.finwise.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryReportData {
    private ReportMetadata metadata;
    private BigDecimal totalExpenses;
    private String formattedTotalExpenses;
    private BigDecimal totalIncome;
    private String formattedTotalIncome;
    private int categoryCount;
    private long transactionCount;

    @Builder.Default
    private List<ReportCategorySummary> categorySummaries = new ArrayList<>();

    @Builder.Default
    private List<ReportTransactionItem> transactions = new ArrayList<>();
}
