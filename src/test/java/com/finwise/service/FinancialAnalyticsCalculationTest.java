package com.finwise.service;

import com.finwise.dto.analytics.*;
import com.finwise.dto.budget.BudgetResponse;
import com.finwise.entity.TransactionType;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Financial Analytics & Health Score Calculation Unit Tests")
class FinancialAnalyticsCalculationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private Long userId;
    private YearMonth testMonth;

    @BeforeEach
    void setUp() {
        userId = 42L;
        testMonth = YearMonth.of(2026, 11);
    }

    @Test
    @DisplayName("Should accurately calculate total income, total expenses, net savings and savings rate")
    void testCoreTotalsAndSavingsRate() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.INCOME), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("100000.00"));

        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.EXPENSE), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("65000.00"));

        BigDecimal income = analyticsService.getTotalIncome(userId, testMonth);
        BigDecimal expense = analyticsService.getTotalExpenses(userId, testMonth);
        BigDecimal savings = analyticsService.getNetSavings(userId, testMonth);
        double savingsRate = analyticsService.getSavingsRate(userId, testMonth);

        assertThat(income).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(expense).isEqualByComparingTo(new BigDecimal("65000.00"));
        assertThat(savings).isEqualByComparingTo(new BigDecimal("35000.00"));
        assertThat(savingsRate).isEqualTo(35.0); // 35,000 / 100,000 * 100
    }

    @Test
    @DisplayName("Should handle zero income gracefully without divide by zero")
    void testZeroIncomeSavingsRate() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.INCOME), any(Instant.class), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);

        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.EXPENSE), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("5000.00"));

        double rate = analyticsService.getSavingsRate(userId, testMonth);
        assertThat(rate).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should accurately categorize Needs vs Wants according to 50/30/20 standard")
    void testNeedsVsWantsBreakdown() {
        // Raw DB rows: [0: id, 1: name, 2: color, 3: icon, 4: sumAmount, 5: count]
        List<Object[]> rawData = List.of(
                new Object[]{1L, "Housing & Rent", "#EF4444", "home", new BigDecimal("30000.00"), 1L},     // Need
                new Object[]{2L, "Groceries & Provisions", "#F59E0B", "cart", new BigDecimal("15000.00"), 4L}, // Need
                new Object[]{3L, "Utilities & Bills", "#8B5CF6", "zap", new BigDecimal("5000.00"), 2L},      // Need
                new Object[]{4L, "Food & Dining Out", "#F97316", "utensils", new BigDecimal("12000.00"), 6L}, // Want
                new Object[]{5L, "Entertainment & OTT", "#6366F1", "film", new BigDecimal("8000.00"), 3L},  // Want
                new Object[]{6L, "Investments & SIPs", "#10B981", "trend", new BigDecimal("20000.00"), 2L}  // Savings/Investment
        );

        when(transactionRepository.findDetailedCategoryExpenseBreakdown(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(rawData);

        NeedsWantsResponse response = analyticsService.getNeedsVsWantsBreakdown(userId, testMonth);

        assertThat(response.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("90000.00"));
        assertThat(response.getNeedsAmount()).isEqualByComparingTo(new BigDecimal("50000.00")); // 30k + 15k + 5k
        assertThat(response.getWantsAmount()).isEqualByComparingTo(new BigDecimal("20000.00")); // 12k + 8k
        assertThat(response.getSavingsOrInvestmentsAmount()).isEqualByComparingTo(new BigDecimal("20000.00")); // 20k

        // 50,000 / 90,000 * 100 = 55.56%
        assertThat(response.getNeedsPercentage()).isEqualTo(55.56);
        // 20,000 / 90,000 * 100 = 22.22%
        assertThat(response.getWantsPercentage()).isEqualTo(22.22);
        // 20,000 / 90,000 * 100 = 22.22%
        assertThat(response.getSavingsOrInvestmentsPercentage()).isEqualTo(22.22);

        assertThat(response.getBenchmarkRule()).contains("50/30/20");
        assertThat(response.getTopNeedsCategories()).hasSize(3);
        assertThat(response.getTopWantsCategories()).hasSize(2);
    }

    @Test
    @DisplayName("Financial Health Score: Should compute correct score for excellent financial discipline")
    void testFinancialHealthScoreCalculationExcellent() {
        // 1. Savings Rate: 35% (earned max 40 points)
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.INCOME), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("100000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.EXPENSE), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("65000.00"));

        // 2. Budget Adherence: 70% utilization (<= 80% earned max 35 points)
        BudgetResponse activeBudget = BudgetResponse.builder()
                .totalBudgetAmount(new BigDecimal("70000.00"))
                .totalSpentAmount(new BigDecimal("49000.00"))
                .utilizationPercentage(70.0)
                .build();
        when(budgetService.getActiveBudget(userId)).thenReturn(activeBudget);

        // 3. Spending Consistency: Low volatility (earned max 25 points)
        // Set previous 3 months total spending
        when(transactionRepository.findDetailedCategoryExpenseBreakdown(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        FinancialHealthScoreResponse score = analyticsService.getFinancialHealthScore(userId, testMonth);

        assertThat(score.getOverallScore()).isGreaterThanOrEqualTo(85);
        assertThat(score.getRatingBand()).isIn("EXCELLENT", "GOOD");
        assertThat(score.getSavingsRateComponent().getEarnedScore()).isEqualTo(40.0);
        assertThat(score.getBudgetAdherenceComponent().getEarnedScore()).isEqualTo(35.0);

        // Required disclaimers and formulas
        assertThat(score.getDisclaimer()).contains("NOT a credit score");
        assertThat(score.getFormulaExplanation()).contains("Savings Rate (40% weight)");
        assertThat(score.getFormulaExplanation()).contains("Budget Adherence (35% weight)");
        assertThat(score.getFormulaExplanation()).contains("Spending Consistency (25% weight)");
    }

    @Test
    @DisplayName("Financial Health Score: Should penalize budget overspending and negative savings rate")
    void testFinancialHealthScoreOverspending() {
        // Income 40,000, Expense 60,000 -> Deficit of 20,000 (-50% savings rate)
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.INCOME), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("40000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.EXPENSE), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("60000.00"));

        // Budget exceeded: 130% utilization
        BudgetResponse activeBudget = BudgetResponse.builder()
                .totalBudgetAmount(new BigDecimal("45000.00"))
                .totalSpentAmount(new BigDecimal("58500.00"))
                .utilizationPercentage(130.0)
                .build();
        when(budgetService.getActiveBudget(userId)).thenReturn(activeBudget);

        FinancialHealthScoreResponse score = analyticsService.getFinancialHealthScore(userId, testMonth);

        // Should receive low earned scores
        assertThat(score.getSavingsRateComponent().getEarnedScore()).isLessThanOrEqualTo(10.0);
        assertThat(score.getBudgetAdherenceComponent().getEarnedScore()).isLessThanOrEqualTo(10.0);
        assertThat(score.getOverallScore()).isLessThan(50);
        assertThat(score.getRatingBand()).isEqualTo("NEEDS_ATTENTION");
    }

    @Test
    @DisplayName("Smart Insights: Should generate rule-based insights for category surges and budget warnings")
    void testSmartInsightsRuleGeneration() {
        // Setup current month data: Income 80k, Expense 50k
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.INCOME), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("80000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), eq(TransactionType.EXPENSE), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("50000.00"));

        // Category breakdown: Food & Dining increased from 5,000 to 10,000 (100% surge)
        List<Object[]> currentCats = List.of(
                new Object[]{1L, "Food & Dining Out", "#F97316", "utensils", new BigDecimal("10000.00"), 5L},
                new Object[]{2L, "Shopping & Clothes", "#14B8A6", "bag", new BigDecimal("8000.00"), 3L}
        );
        when(transactionRepository.findDetailedCategoryExpenseBreakdown(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(currentCats);

        // Budget at 85% utilization (warning trigger)
        BudgetResponse budget = BudgetResponse.builder()
                .totalBudgetAmount(new BigDecimal("55000.00"))
                .totalSpentAmount(new BigDecimal("46750.00"))
                .utilizationPercentage(85.0)
                .build();
        when(budgetService.getActiveBudget(userId)).thenReturn(budget);

        SmartInsightResponse response = analyticsService.getSmartInsights(userId, testMonth);

        assertThat(response.isHasEnoughData()).isTrue();
        assertThat(response.getInsights()).isNotEmpty();

        // Check for budget warning
        boolean hasBudgetAlert = response.getInsights().stream()
                .anyMatch(i -> i.getType().equals("BUDGET_ALERT") && i.getMessage().contains("crossed 85%"));
        assertThat(hasBudgetAlert).isTrue();

        // Check for shopping optimization
        boolean hasShoppingOptimization = response.getInsights().stream()
                .anyMatch(i -> i.getType().equals("DISCRETIONARY_OPTIMIZATION"));
        assertThat(hasShoppingOptimization).isTrue();

        // Check for surplus alert
        boolean hasSurplusAlert = response.getInsights().stream()
                .anyMatch(i -> i.getType().equals("SURPLUS_ALERT"));
        assertThat(hasSurplusAlert).isTrue();
    }

    @Test
    @DisplayName("Smart Insights: Should handle new accounts with zero transactions gracefully")
    void testSmartInsightsInsufficientData() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                eq(userId), any(TransactionType.class), any(Instant.class), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);

        SmartInsightResponse response = analyticsService.getSmartInsights(userId, testMonth);

        assertThat(response.isHasEnoughData()).isFalse();
        assertThat(response.getInsights()).hasSize(1);
        assertThat(response.getInsights().get(0).getTitle()).contains("Welcome");
    }
}
