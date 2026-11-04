package com.finwise.controller;

import com.finwise.dto.analytics.*;
import com.finwise.dto.response.ApiResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    // 1. Total Income
    @GetMapping("/total-income")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTotalIncome(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching total income for user id: {}, period: {}", principal.getId(), ym);
        BigDecimal income = analyticsService.getTotalIncome(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "period", ym.toString(),
                "totalIncome", income,
                "currencyCode", "INR"
        )));
    }

    // 2. Total Expenses
    @GetMapping("/total-expenses")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTotalExpenses(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching total expenses for user id: {}, period: {}", principal.getId(), ym);
        BigDecimal expense = analyticsService.getTotalExpenses(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "period", ym.toString(),
                "totalExpenses", expense,
                "currencyCode", "INR"
        )));
    }

    // 3. Net Savings
    @GetMapping("/net-savings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNetSavings(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching net savings for user id: {}, period: {}", principal.getId(), ym);
        BigDecimal savings = analyticsService.getNetSavings(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "period", ym.toString(),
                "netSavings", savings,
                "currencyCode", "INR"
        )));
    }

    // 4. Savings Rate
    @GetMapping("/savings-rate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSavingsRate(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching savings rate for user id: {}, period: {}", principal.getId(), ym);
        double rate = analyticsService.getSavingsRate(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "period", ym.toString(),
                "savingsRate", rate
        )));
    }

    // 5. Monthly Spending Trends
    @GetMapping("/spending-trends")
    public ResponseEntity<ApiResponse<List<MonthlySpendingTrendResponse>>> getSpendingTrends(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching {} months spending trends for user id: {}", months, principal.getId());
        List<MonthlySpendingTrendResponse> trends = analyticsService.getSpendingTrends(principal.getId(), months);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    // 6. Category-wise Expenses
    @GetMapping("/category-expenses")
    public ResponseEntity<ApiResponse<List<CategoryExpenseResponse>>> getCategoryExpenses(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching category expenses for user id: {}, period: {}", principal.getId(), ym);
        List<CategoryExpenseResponse> breakdown = analyticsService.getCategoryExpenses(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(breakdown));
    }

    // 7. Income versus Expense Comparison
    @GetMapping("/income-vs-expense")
    public ResponseEntity<ApiResponse<List<IncomeVsExpenseResponse>>> getIncomeVsExpenseComparison(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching income vs expense comparison for user id: {}, months: {}", principal.getId(), months);
        List<IncomeVsExpenseResponse> response = analyticsService.getIncomeVsExpenseComparison(principal.getId(), months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 8. Needs versus Wants Breakdown
    @GetMapping("/needs-vs-wants")
    public ResponseEntity<ApiResponse<NeedsWantsResponse>> getNeedsVsWants(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching needs vs wants breakdown for user id: {}, period: {}", principal.getId(), ym);
        NeedsWantsResponse response = analyticsService.getNeedsVsWantsBreakdown(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 9. Previous Month Comparison
    @GetMapping({"/previous-month-comparison", "/month-comparison"})
    public ResponseEntity<ApiResponse<MonthComparisonResponse>> getMonthComparison(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching month comparison for user id: {}, period: {}", principal.getId(), ym);
        MonthComparisonResponse response = analyticsService.getMonthComparison(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 10. Financial Health Score
    @GetMapping("/financial-health-score")
    public ResponseEntity<ApiResponse<FinancialHealthScoreResponse>> getFinancialHealthScore(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Calculating financial health score for user id: {}, period: {}", principal.getId(), ym);
        FinancialHealthScoreResponse response = analyticsService.getFinancialHealthScore(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 11. Smart Insights
    @GetMapping({"/smart-insights", "/insights"})
    public ResponseEntity<ApiResponse<SmartInsightResponse>> getSmartInsights(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Generating smart insights for user id: {}, period: {}", principal.getId(), ym);
        SmartInsightResponse response = analyticsService.getSmartInsights(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Comprehensive Overview Summary
    @GetMapping({"/summary", "/overview"})
    public ResponseEntity<ApiResponse<OverviewSummaryResponse>> getOverviewSummary(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        YearMonth ym = parseYearMonth(month);
        log.debug("Fetching overview summary for user id: {}, period: {}", principal.getId(), ym);
        OverviewSummaryResponse response = analyticsService.getOverviewSummary(principal.getId(), ym);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Backward-Compatible Endpoints
    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching dashboard summary for user id: {}", principal.getId());
        DashboardSummaryResponse response = analyticsService.getDashboardSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<ApiResponse<List<CashFlowResponse>>> getCashFlow(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching {} months cash flow for user id: {}", months, principal.getId());
        List<CashFlowResponse> response = analyticsService.getCashFlow(principal.getId(), months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<ApiResponse<List<CategoryExpenseResponse>>> getCategoryBreakdown(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching category breakdown for user id: {}", principal.getId());
        List<CategoryExpenseResponse> response = analyticsService.getCategoryExpenses(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private YearMonth parseYearMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        try {
            return YearMonth.parse(monthStr.trim());
        } catch (Exception e) {
            log.warn("Invalid month format '{}', defaulting to current month", monthStr);
            return YearMonth.now(ZoneOffset.UTC);
        }
    }
}
