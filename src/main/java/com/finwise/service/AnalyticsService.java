package com.finwise.service;

import com.finwise.dto.analytics.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public interface AnalyticsService {

    // 1. Core Totals & Rates
    OverviewSummaryResponse getOverviewSummary(Long userId, YearMonth month);

    BigDecimal getTotalIncome(Long userId, YearMonth month);

    BigDecimal getTotalExpenses(Long userId, YearMonth month);

    BigDecimal getNetSavings(Long userId, YearMonth month);

    double getSavingsRate(Long userId, YearMonth month);

    // 2. Spending Trends & Category Distributions
    List<MonthlySpendingTrendResponse> getSpendingTrends(Long userId, int months);

    List<CategoryExpenseResponse> getCategoryExpenses(Long userId, YearMonth month);

    // 3. Multi-Month & Structural Comparisons
    List<IncomeVsExpenseResponse> getIncomeVsExpenseComparison(Long userId, int months);

    NeedsWantsResponse getNeedsVsWantsBreakdown(Long userId, YearMonth month);

    MonthComparisonResponse getMonthComparison(Long userId, YearMonth month);

    // 4. Educational Financial Health Score
    FinancialHealthScoreResponse getFinancialHealthScore(Long userId, YearMonth month);

    // 5. Rule-Based Smart Insights
    SmartInsightResponse getSmartInsights(Long userId, YearMonth month);

    // Backward Compatible Dashboard Endpoints
    DashboardSummaryResponse getDashboardSummary(Long userId);

    List<CashFlowResponse> getCashFlow(Long userId, int months);

    List<CategoryExpenseResponse> getCategoryExpenses(Long userId);
}
