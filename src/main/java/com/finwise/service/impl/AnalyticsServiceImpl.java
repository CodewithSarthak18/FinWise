package com.finwise.service.impl;

import com.finwise.dto.analytics.*;
import com.finwise.dto.budget.BudgetResponse;
import com.finwise.entity.TransactionType;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.service.AnalyticsService;
import com.finwise.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    private static final Set<String> NEEDS_KEYWORDS = Set.of(
            "rent", "housing", "grocer", "provis", "utilit", "bill", "electr", "water",
            "transport", "fuel", "petrol", "diesel", "health", "medic", "doctor", "hospital",
            "educat", "book", "school", "college", "tuition", "insuran", "emi", "loan",
            "internet", "broadband", "wifi", "maid", "maintenance"
    );

    private static final Set<String> SAVINGS_KEYWORDS = Set.of(
            "invest", "sip", "mutual fund", "stock", "deposit", "gold", "epf", "ppf", "nps", "crypto"
    );

    @Override
    @Transactional(readOnly = true)
    public OverviewSummaryResponse getOverviewSummary(Long userId, YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        BigDecimal income = getTotalIncome(userId, targetMonth);
        BigDecimal expense = getTotalExpenses(userId, targetMonth);
        BigDecimal savings = income.subtract(expense);
        double savingsRate = calculateSavingsRate(income, savings);

        BigDecimal netWorth = accountRepository.calculateTotalNetWorth(userId);
        if (netWorth == null) {
            netWorth = BigDecimal.ZERO;
        }

        long txCount = transactionRepository.countByUserIdAndTransactionDateBetween(userId, start, end);
        BudgetResponse activeBudget = budgetService.getActiveBudget(userId);

        return OverviewSummaryResponse.builder()
                .period(targetMonth.toString())
                .totalIncome(income)
                .totalExpenses(expense)
                .netSavings(savings)
                .savingsRate(savingsRate)
                .netWorth(netWorth)
                .currencyCode("INR")
                .transactionCount(txCount)
                .hasActiveBudget(activeBudget != null)
                .budgetUtilization(activeBudget != null ? activeBudget.getUtilizationPercentage() : 0.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalIncome(Long userId, YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        BigDecimal income = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.INCOME, start, end);
        return income != null ? income : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(Long userId, YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        BigDecimal expense = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.EXPENSE, start, end);
        BigDecimal refund = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.REFUND, start, end);
        BigDecimal gross = expense != null ? expense : BigDecimal.ZERO;
        BigDecimal ref = refund != null ? refund : BigDecimal.ZERO;
        return gross.subtract(ref).max(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getNetSavings(Long userId, YearMonth month) {
        BigDecimal income = getTotalIncome(userId, month);
        BigDecimal expense = getTotalExpenses(userId, month);
        return income.subtract(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public double getSavingsRate(Long userId, YearMonth month) {
        BigDecimal income = getTotalIncome(userId, month);
        BigDecimal expense = getTotalExpenses(userId, month);
        BigDecimal savings = income.subtract(expense);
        return calculateSavingsRate(income, savings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlySpendingTrendResponse> getSpendingTrends(Long userId, int months) {
        int window = months > 0 ? Math.min(months, 24) : 6;
        List<MonthlySpendingTrendResponse> trends = new ArrayList<>();
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = window - 1; i >= 0; i--) {
            YearMonth targetMonth = current.minusMonths(i);
            Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

            BigDecimal spending = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    userId, TransactionType.EXPENSE, start, end);
            if (spending == null) spending = BigDecimal.ZERO;

            long txCount = transactionRepository.countByUserIdAndTransactionDateBetween(userId, start, end);
            int days = targetMonth.lengthOfMonth();
            BigDecimal dailyAvg = spending.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_EVEN);

            List<Object[]> rawCats = transactionRepository.findDetailedCategoryExpenseBreakdown(userId, start, end);
            String topCat = "None";
            BigDecimal topAmt = BigDecimal.ZERO;
            if (!rawCats.isEmpty()) {
                Object[] firstRow = rawCats.get(0);
                topCat = (String) firstRow[1];
                topAmt = (BigDecimal) firstRow[4];
            }

            trends.add(MonthlySpendingTrendResponse.builder()
                    .monthYear(targetMonth.toString())
                    .label(targetMonth.format(labelFormatter))
                    .totalSpending(spending)
                    .dailyAverage(dailyAvg)
                    .transactionCount(txCount)
                    .highestExpenseCategory(topCat)
                    .highestExpenseAmount(topAmt)
                    .build());
        }

        return trends;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryExpenseResponse> getCategoryExpenses(Long userId, YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        List<Object[]> rawData = transactionRepository.findDetailedCategoryExpenseBreakdown(userId, start, end);
        if (rawData.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : rawData) {
            BigDecimal amt = (BigDecimal) row[4];
            if (amt != null) total = total.add(amt);
        }

        List<CategoryExpenseResponse> result = new ArrayList<>();
        for (Object[] row : rawData) {
            Long catId = (Long) row[0];
            String name = (String) row[1];
            String color = (String) row[2];
            String icon = (String) row[3];
            BigDecimal amt = (BigDecimal) row[4];
            long count = ((Number) row[5]).longValue();

            double pct = 0.0;
            if (total.compareTo(BigDecimal.ZERO) > 0 && amt != null) {
                pct = amt.multiply(new BigDecimal("100"))
                        .divide(total, 2, RoundingMode.HALF_EVEN)
                        .doubleValue();
            }

            result.add(CategoryExpenseResponse.builder()
                    .categoryId(catId)
                    .categoryName(name)
                    .colorCode(color)
                    .icon(icon)
                    .amount(amt)
                    .percentage(pct)
                    .transactionCount(count)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomeVsExpenseResponse> getIncomeVsExpenseComparison(Long userId, int months) {
        int window = months > 0 ? Math.min(months, 24) : 6;
        List<IncomeVsExpenseResponse> list = new ArrayList<>();
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = window - 1; i >= 0; i--) {
            YearMonth targetMonth = current.minusMonths(i);
            Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

            BigDecimal inc = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    userId, TransactionType.INCOME, start, end);
            if (inc == null) inc = BigDecimal.ZERO;

            BigDecimal exp = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    userId, TransactionType.EXPENSE, start, end);
            if (exp == null) exp = BigDecimal.ZERO;

            BigDecimal net = inc.subtract(exp);
            double rate = calculateSavingsRate(inc, net);

            String status = "BALANCED";
            if (inc.compareTo(exp) > 0) {
                status = "SURPLUS";
            } else if (inc.compareTo(exp) < 0) {
                status = "DEFICIT";
            }

            list.add(IncomeVsExpenseResponse.builder()
                    .monthYear(targetMonth.toString())
                    .label(targetMonth.format(labelFormatter))
                    .income(inc)
                    .expense(exp)
                    .netSavings(net)
                    .savingsRate(rate)
                    .status(status)
                    .build());
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public NeedsWantsResponse getNeedsVsWantsBreakdown(Long userId, YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        List<CategoryExpenseResponse> allExpenses = getCategoryExpenses(userId, targetMonth);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal needsTotal = BigDecimal.ZERO;
        BigDecimal wantsTotal = BigDecimal.ZERO;
        BigDecimal savingsTotal = BigDecimal.ZERO;

        List<CategoryExpenseResponse> needsList = new ArrayList<>();
        List<CategoryExpenseResponse> wantsList = new ArrayList<>();

        for (CategoryExpenseResponse cat : allExpenses) {
            BigDecimal amt = cat.getAmount();
            total = total.add(amt);
            String nameLower = cat.getCategoryName().toLowerCase(Locale.ROOT);

            if (isNeedsCategory(nameLower)) {
                needsTotal = needsTotal.add(amt);
                needsList.add(cat);
            } else if (isSavingsCategory(nameLower)) {
                savingsTotal = savingsTotal.add(amt);
            } else {
                wantsTotal = wantsTotal.add(amt);
                wantsList.add(cat);
            }
        }

        double needsPct = calculatePercentage(needsTotal, total);
        double wantsPct = calculatePercentage(wantsTotal, total);
        double savingsPct = calculatePercentage(savingsTotal, total);

        String analysis;
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            analysis = "No expense records found for this period to evaluate needs versus wants.";
        } else if (needsPct <= 50.0 && wantsPct <= 30.0) {
            analysis = "Superb adherence to the 50/30/20 guideline! Your essential needs and discretionary wants are strictly under control.";
        } else if (wantsPct > 35.0) {
            analysis = "Discretionary spending ('Wants') accounts for " + wantsPct +
                    "% of expenses (benchmark: 30%). Reducing dining out, entertainment, or shopping can boost your savings.";
        } else if (needsPct > 65.0) {
            analysis = "Essential needs consume " + needsPct +
                    "% of your total expenses (benchmark: 50%). Consider reviewing high fixed obligations like rent, utilities, or transport.";
        } else {
            analysis = "Your expenditure distribution is balanced: Needs (" + needsPct + "%), Wants (" + wantsPct +
                    "%), and Investments (" + savingsPct + "%).";
        }

        return NeedsWantsResponse.builder()
                .period(targetMonth.toString())
                .totalExpenses(total)
                .needsAmount(needsTotal)
                .needsPercentage(needsPct)
                .wantsAmount(wantsTotal)
                .wantsPercentage(wantsPct)
                .savingsOrInvestmentsAmount(savingsTotal)
                .savingsOrInvestmentsPercentage(savingsPct)
                .benchmarkRule("50/30/20 Rule: 50% Needs, 30% Wants, 20% Savings & Investments")
                .analysis(analysis)
                .topNeedsCategories(needsList)
                .topWantsCategories(wantsList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthComparisonResponse getMonthComparison(Long userId, YearMonth month) {
        YearMonth current = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        YearMonth previous = current.minusMonths(1);

        BigDecimal curInc = getTotalIncome(userId, current);
        BigDecimal prevInc = getTotalIncome(userId, previous);
        BigDecimal incDiff = curInc.subtract(prevInc);
        double incChangePct = calculateChangePercentage(curInc, prevInc);

        BigDecimal curExp = getTotalExpenses(userId, current);
        BigDecimal prevExp = getTotalExpenses(userId, previous);
        BigDecimal expDiff = curExp.subtract(prevExp);
        double expChangePct = calculateChangePercentage(curExp, prevExp);

        BigDecimal curSav = curInc.subtract(curExp);
        BigDecimal prevSav = prevInc.subtract(prevExp);
        BigDecimal savDiff = curSav.subtract(prevSav);
        double savChangePct = calculateChangePercentage(curSav, prevSav);

        double curRate = calculateSavingsRate(curInc, curSav);
        double prevRate = calculateSavingsRate(prevInc, prevSav);
        double rateDiff = BigDecimal.valueOf(curRate - prevRate).setScale(2, RoundingMode.HALF_EVEN).doubleValue();

        // Category by category comparison
        List<CategoryExpenseResponse> curCats = getCategoryExpenses(userId, current);
        List<CategoryExpenseResponse> prevCats = getCategoryExpenses(userId, previous);

        Map<String, BigDecimal> curMap = curCats.stream()
                .collect(Collectors.toMap(CategoryExpenseResponse::getCategoryName, CategoryExpenseResponse::getAmount, (a, b) -> a));
        Map<String, BigDecimal> prevMap = prevCats.stream()
                .collect(Collectors.toMap(CategoryExpenseResponse::getCategoryName, CategoryExpenseResponse::getAmount, (a, b) -> a));

        Set<String> allCategoryNames = new HashSet<>();
        allCategoryNames.addAll(curMap.keySet());
        allCategoryNames.addAll(prevMap.keySet());

        List<CategoryComparisonItem> increases = new ArrayList<>();
        List<CategoryComparisonItem> decreases = new ArrayList<>();

        for (String catName : allCategoryNames) {
            BigDecimal cAmt = curMap.getOrDefault(catName, BigDecimal.ZERO);
            BigDecimal pAmt = prevMap.getOrDefault(catName, BigDecimal.ZERO);
            BigDecimal diff = cAmt.subtract(pAmt);
            double pct = calculateChangePercentage(cAmt, pAmt);

            CategoryComparisonItem item = CategoryComparisonItem.builder()
                    .categoryName(catName)
                    .currentAmount(cAmt)
                    .previousAmount(pAmt)
                    .difference(diff)
                    .percentageChange(pct)
                    .build();

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                increases.add(item);
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                decreases.add(item);
            }
        }

        increases.sort(Comparator.comparing(CategoryComparisonItem::getDifference).reversed());
        decreases.sort(Comparator.comparing(CategoryComparisonItem::getDifference));

        return MonthComparisonResponse.builder()
                .currentMonth(current.toString())
                .previousMonth(previous.toString())
                .currentIncome(curInc)
                .previousIncome(prevInc)
                .incomeDifference(incDiff)
                .incomeChangePercentage(incChangePct)
                .currentExpense(curExp)
                .previousExpense(prevExp)
                .expenseDifference(expDiff)
                .expenseChangePercentage(expChangePct)
                .currentSavings(curSav)
                .previousSavings(prevSav)
                .savingsDifference(savDiff)
                .savingsChangePercentage(savChangePct)
                .currentSavingsRate(curRate)
                .previousSavingsRate(prevRate)
                .savingsRateDifference(rateDiff)
                .topIncreasedCategories(increases)
                .topDecreasedCategories(decreases)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialHealthScoreResponse getFinancialHealthScore(Long userId, YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);

        // 1. Savings Rate Component (Weight: 40 points)
        BigDecimal income = getTotalIncome(userId, targetMonth);
        BigDecimal expense = getTotalExpenses(userId, targetMonth);
        BigDecimal savings = income.subtract(expense);
        double savingsRate = calculateSavingsRate(income, savings);

        double savingsScore;
        String savingsEval;
        String savingsRec;

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            if (expense.compareTo(BigDecimal.ZERO) == 0) {
                savingsScore = 20.0;
                savingsEval = "No income or expense records found for this month.";
                savingsRec = "Log your monthly income to activate personalized savings evaluation.";
            } else {
                savingsScore = 0.0;
                savingsEval = "Deficit spending: Expenses occurred without recorded income.";
                savingsRec = "Record your primary income sources to balance your cash flow.";
            }
        } else if (savingsRate >= 30.0) {
            savingsScore = 40.0;
            savingsEval = "Exceptional savings rate of " + savingsRate + "% (Target: >= 30%).";
            savingsRec = "Maintain your stellar savings rate and consider channeling surplus into investments.";
        } else if (savingsRate >= 20.0) {
            savingsScore = 30.0 + (10.0 * (savingsRate - 20.0) / 10.0);
            savingsEval = "Healthy savings rate of " + savingsRate + "% (Target: >= 20%).";
            savingsRec = "You meet the standard financial benchmark of saving at least 20% of your earnings.";
        } else if (savingsRate >= 10.0) {
            savingsScore = 20.0 + (10.0 * (savingsRate - 10.0) / 10.0);
            savingsEval = "Moderate savings rate of " + savingsRate + "% (Target: 10% - 20%).";
            savingsRec = "Aim to raise your savings rate above 20% by curbing discretionary expenses.";
        } else if (savingsRate >= 0.0) {
            savingsScore = 10.0 + (10.0 * savingsRate / 10.0);
            savingsEval = "Low savings rate of " + savingsRate + "% (Target: >= 20%).";
            savingsRec = "Your savings buffer is thin. Focus on building an emergency reserve.";
        } else {
            savingsScore = Math.max(0.0, 10.0 + (savingsRate / 2.0));
            savingsEval = "Deficit alert: Expenses exceeded income (Savings Rate: " + savingsRate + "%).";
            savingsRec = "Immediate budget review recommended: cut non-essential spending to avoid debt.";
        }
        savingsScore = Math.min(40.0, Math.max(0.0, savingsScore));

        ScoreComponent savingsComp = ScoreComponent.builder()
                .name("Savings Rate")
                .earnedScore(roundToOneDecimal(savingsScore))
                .maxScore(40.0)
                .weightPercentage(40)
                .metricValue(savingsRate + "%")
                .evaluation(savingsEval)
                .recommendation(savingsRec)
                .build();

        // 2. Budget Adherence Component (Weight: 35 points)
        BudgetResponse activeBudget = budgetService.getActiveBudget(userId);
        double budgetScore;
        String budgetEval;
        String budgetRec;
        String budgetMetric;

        if (activeBudget == null) {
            budgetScore = 20.0; // Neutral default
            budgetMetric = "No Active Budget";
            budgetEval = "No active monthly budget is currently set.";
            budgetRec = "Create a monthly budget to unlock active tracking and earn up to 35 points.";
        } else {
            double util = activeBudget.getUtilizationPercentage();
            budgetMetric = util + "% of ₹" + activeBudget.getTotalBudgetAmount();

            if (util <= 80.0) {
                budgetScore = 35.0;
                budgetEval = "Outstanding budget adherence! You have used " + util + "% of your budget.";
                budgetRec = "You are pacing comfortably below your spending limits.";
            } else if (util <= 100.0) {
                budgetScore = 20.0 + (15.0 * (100.0 - util) / 20.0);
                budgetEval = "Approaching budget ceiling: " + util + "% utilized.";
                budgetRec = "Slow down discretionary outflow to stay within your remaining allocation.";
            } else if (util <= 120.0) {
                budgetScore = 10.0 + (10.0 * (120.0 - util) / 20.0);
                budgetEval = "Budget exceeded by " + (util - 100.0) + "%.";
                budgetRec = "Review overspent categories and reallocate funds where possible.";
            } else {
                budgetScore = Math.max(0.0, 10.0 - ((util - 120.0) * 0.2));
                budgetEval = "Critical overspending: Budget exceeded by " + (util - 100.0) + "%.";
                budgetRec = "Immediate freeze on non-essential expenditures suggested.";
            }
        }
        budgetScore = Math.min(35.0, Math.max(0.0, budgetScore));

        ScoreComponent budgetComp = ScoreComponent.builder()
                .name("Budget Adherence")
                .earnedScore(roundToOneDecimal(budgetScore))
                .maxScore(35.0)
                .weightPercentage(35)
                .metricValue(budgetMetric)
                .evaluation(budgetEval)
                .recommendation(budgetRec)
                .build();

        // 3. Spending Consistency Component (Weight: 25 points)
        double consistencyScore;
        String consistencyEval;
        String consistencyRec;
        String consistencyMetric;

        List<MonthlySpendingTrendResponse> history = getSpendingTrends(userId, 3);
        List<BigDecimal> nonZeroExpenses = history.stream()
                .map(MonthlySpendingTrendResponse::getTotalSpending)
                .filter(amt -> amt.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (nonZeroExpenses.size() < 2) {
            consistencyScore = 18.0; // Graceful neutral baseline for new accounts
            consistencyMetric = "Initial History";
            consistencyEval = "Limited expenditure history across previous months.";
            consistencyRec = "Continue tracking your regular monthly spending to establish baseline consistency.";
        } else {
            double sum = 0.0;
            for (BigDecimal b : nonZeroExpenses) sum += b.doubleValue();
            double mean = sum / nonZeroExpenses.size();

            double varianceSum = 0.0;
            for (BigDecimal b : nonZeroExpenses) {
                double diff = b.doubleValue() - mean;
                varianceSum += diff * diff;
            }
            double stdDev = Math.sqrt(varianceSum / nonZeroExpenses.size());
            double cv = mean > 0 ? (stdDev / mean) : 0.0;
            consistencyMetric = "Volatility: " + Math.round(cv * 100) + "%";

            if (cv <= 0.15) {
                consistencyScore = 25.0;
                consistencyEval = "Very high spending predictability month over month.";
                consistencyRec = "Your expense profile is remarkably steady and free from volatile surprises.";
            } else if (cv <= 0.30) {
                consistencyScore = 20.0;
                consistencyEval = "Moderate expense consistency with minor seasonal variation.";
                consistencyRec = "Maintain a steady baseline for routine monthly bills.";
            } else if (cv <= 0.50) {
                consistencyScore = 14.0;
                consistencyEval = "Fluctuating expenses observed across recent months.";
                consistencyRec = "Look into smoothing large one-off purchases across sinking funds.";
            } else {
                consistencyScore = 8.0;
                consistencyEval = "High volatility and spending spikes detected.";
                consistencyRec = "Identify irregular expenditure spikes and plan ahead with sinking funds.";
            }
        }
        consistencyScore = Math.min(25.0, Math.max(0.0, consistencyScore));

        ScoreComponent consistencyComp = ScoreComponent.builder()
                .name("Spending Consistency")
                .earnedScore(roundToOneDecimal(consistencyScore))
                .maxScore(25.0)
                .weightPercentage(25)
                .metricValue(consistencyMetric)
                .evaluation(consistencyEval)
                .recommendation(consistencyRec)
                .build();

        int overall = (int) Math.round(savingsScore + budgetScore + consistencyScore);
        overall = Math.min(100, Math.max(0, overall));

        String ratingBand;
        String summary;
        if (overall >= 80) {
            ratingBand = "EXCELLENT";
            summary = "Outstanding financial discipline! Your savings rate, budget control, and spending stability are exemplary.";
        } else if (overall >= 65) {
            ratingBand = "GOOD";
            summary = "Healthy financial habits with dependable stability. A few minor optimizations can push you into top tier.";
        } else if (overall >= 50) {
            ratingBand = "FAIR";
            summary = "Fair financial balance. Focus on boosting your monthly savings cushion and setting stricter category budgets.";
        } else {
            ratingBand = "NEEDS_ATTENTION";
            summary = "Your financial balance requires attention due to low savings or budget overruns. Review discretionary spending.";
        }

        List<String> actionItems = new ArrayList<>();
        if (savingsComp.getRecommendation() != null) actionItems.add(savingsComp.getRecommendation());
        if (budgetComp.getRecommendation() != null) actionItems.add(budgetComp.getRecommendation());
        if (consistencyComp.getRecommendation() != null) actionItems.add(consistencyComp.getRecommendation());

        return FinancialHealthScoreResponse.builder()
                .overallScore(overall)
                .ratingBand(ratingBand)
                .summary(summary)
                .savingsRateComponent(savingsComp)
                .budgetAdherenceComponent(budgetComp)
                .spendingConsistencyComponent(consistencyComp)
                .formulaExplanation("FinWise Financial Health Score is an educational index (0-100) calculated from three pillars: (1) Savings Rate (40% weight): rewards saving 20-30%+ of income; (2) Budget Adherence (35% weight): evaluates adherence to planned spending limits; (3) Spending Consistency (25% weight): measures month-over-month expenditure stability.")
                .disclaimer("This is an educational score designed solely for personal financial awareness and habit tracking. It is NOT a credit score, credit rating, or professional financial assessment.")
                .keyActionItems(actionItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SmartInsightResponse getSmartInsights(Long userId, YearMonth month) {
        YearMonth current = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        YearMonth previous = current.minusMonths(1);

        BigDecimal curInc = getTotalIncome(userId, current);
        BigDecimal curExp = getTotalExpenses(userId, current);
        BigDecimal prevInc = getTotalIncome(userId, previous);
        BigDecimal prevExp = getTotalExpenses(userId, previous);

        List<SmartInsightItem> insights = new ArrayList<>();
        int insightCounter = 1;

        // Check for sufficient data
        boolean hasData = curInc.compareTo(BigDecimal.ZERO) > 0 || curExp.compareTo(BigDecimal.ZERO) > 0
                || prevInc.compareTo(BigDecimal.ZERO) > 0 || prevExp.compareTo(BigDecimal.ZERO) > 0;

        if (!hasData) {
            insights.add(SmartInsightItem.builder()
                    .id("INSIGHT-" + (insightCounter++))
                    .type("INFO")
                    .title("Welcome to FinWise Analytics")
                    .message("Add your first income and expense transactions to start generating tailored insights, budget comparisons, and health tracking.")
                    .severity("INFO")
                    .actionable(true)
                    .category("Getting Started")
                    .build());

            return SmartInsightResponse.builder()
                    .period(current.toString())
                    .hasEnoughData(false)
                    .totalInsightsCount(insights.size())
                    .insights(insights)
                    .build();
        }

        // Rule 1: Category Surge (e.g. food expenses increased by 20%)
        List<CategoryExpenseResponse> curCats = getCategoryExpenses(userId, current);
        List<CategoryExpenseResponse> prevCats = getCategoryExpenses(userId, previous);

        Map<String, BigDecimal> prevCatMap = prevCats.stream()
                .collect(Collectors.toMap(CategoryExpenseResponse::getCategoryName, CategoryExpenseResponse::getAmount, (a, b) -> a));

        for (CategoryExpenseResponse cat : curCats) {
            BigDecimal prevAmt = prevCatMap.getOrDefault(cat.getCategoryName(), BigDecimal.ZERO);
            BigDecimal curAmt = cat.getAmount();

            if (prevAmt.compareTo(BigDecimal.ZERO) > 0 && curAmt.compareTo(prevAmt) > 0) {
                BigDecimal diff = curAmt.subtract(prevAmt);
                double pct = calculateChangePercentage(curAmt, prevAmt);

                // Highlight if increase is >= 15% and >= ₹500
                if (pct >= 15.0 && diff.compareTo(new BigDecimal("500.00")) >= 0) {
                    insights.add(SmartInsightItem.builder()
                            .id("INSIGHT-" + (insightCounter++))
                            .type("CATEGORY_SURGE")
                            .title(cat.getCategoryName() + " Spending Surge")
                            .message("Your " + cat.getCategoryName().toLowerCase(Locale.ROOT) +
                                    " expenses increased by " + Math.round(pct) + "% this month (₹" + curAmt +
                                    " vs ₹" + prevAmt + " last month).")
                            .severity("WARNING")
                            .actionable(true)
                            .category(cat.getCategoryName())
                            .build());
                }
            }
        }

        // Rule 2: Budget Adherence (e.g. crossed 80% or exceeded 100%)
        BudgetResponse activeBudget = budgetService.getActiveBudget(userId);
        if (activeBudget != null) {
            double util = activeBudget.getUtilizationPercentage();
            if (util >= 100.0) {
                insights.add(SmartInsightItem.builder()
                        .id("INSIGHT-" + (insightCounter++))
                        .type("BUDGET_ALERT")
                        .title("Monthly Budget Exceeded")
                        .message("You have exceeded your total monthly budget by " +
                                Math.round(util - 100.0) + "% (Spent: ₹" + activeBudget.getTotalSpentAmount() +
                                " of ₹" + activeBudget.getTotalBudgetAmount() + ").")
                        .severity("ALERT")
                        .actionable(true)
                        .category("Budget")
                        .build());
            } else if (util >= 80.0) {
                insights.add(SmartInsightItem.builder()
                        .id("INSIGHT-" + (insightCounter++))
                        .type("BUDGET_ALERT")
                        .title("Budget Warning Threshold")
                        .message("You have crossed " + Math.round(util) +
                                "% of your monthly budget. Watch your discretionary spending for the rest of the month.")
                        .severity("WARNING")
                        .actionable(true)
                        .category("Budget")
                        .build());
            } else if (util < 50.0 && YearMonth.now().equals(current)) {
                insights.add(SmartInsightItem.builder()
                        .id("INSIGHT-" + (insightCounter++))
                        .type("BUDGET_ALERT")
                        .title("Great Budget Discipline")
                        .message("You have utilized only " + Math.round(util) +
                                "% of your budget so far this month. Keep up the strong spending control!")
                        .severity("SUCCESS")
                        .actionable(false)
                        .category("Budget")
                        .build());
            }
        }

        // Rule 3: Discretionary / Shopping Optimization
        NeedsWantsResponse needsWants = getNeedsVsWantsBreakdown(userId, current);
        if (needsWants.getTopWantsCategories() != null && !needsWants.getTopWantsCategories().isEmpty()) {
            CategoryExpenseResponse topWant = needsWants.getTopWantsCategories().get(0);
            if (topWant.getAmount().compareTo(new BigDecimal("1000.00")) >= 0) {
                insights.add(SmartInsightItem.builder()
                        .id("INSIGHT-" + (insightCounter++))
                        .type("DISCRETIONARY_OPTIMIZATION")
                        .title("Savings Opportunity in " + topWant.getCategoryName())
                        .message("You can save more by reducing " + topWant.getCategoryName().toLowerCase(Locale.ROOT) +
                                " expenses (₹" + topWant.getAmount() + " spent, " + topWant.getPercentage() +
                                "% of your total expenditure).")
                        .severity("INFO")
                        .actionable(true)
                        .category(topWant.getCategoryName())
                        .build());
            }
        }

        // Rule 4: Savings Rate Improvement
        double curRate = calculateSavingsRate(curInc, curInc.subtract(curExp));
        double prevRate = calculateSavingsRate(prevInc, prevInc.subtract(prevExp));

        if (prevInc.compareTo(BigDecimal.ZERO) > 0 && curInc.compareTo(BigDecimal.ZERO) > 0) {
            double rateDiff = curRate - prevRate;
            if (rateDiff >= 3.0) {
                insights.add(SmartInsightItem.builder()
                        .id("INSIGHT-" + (insightCounter++))
                        .type("SAVINGS_IMPROVEMENT")
                        .title("Savings Rate Improved")
                        .message("Your savings rate improved to " + curRate +
                                "% this month (up by " + roundToOneDecimal(rateDiff) + "% from " + prevRate + "% last month).")
                        .severity("SUCCESS")
                        .actionable(false)
                        .category("Savings")
                        .build());
            } else if (rateDiff <= -5.0) {
                insights.add(SmartInsightItem.builder()
                        .id("INSIGHT-" + (insightCounter++))
                        .type("SAVINGS_IMPROVEMENT")
                        .title("Savings Rate Decreased")
                        .message("Your savings rate fell to " + curRate +
                                "% this month (down from " + prevRate + "% last month). Review recent discretionary bills.")
                        .severity("WARNING")
                        .actionable(true)
                        .category("Savings")
                        .build());
            }
        }

        // Rule 5: Net Surplus / Deficit Insight
        BigDecimal net = curInc.subtract(curExp);
        if (net.compareTo(BigDecimal.ZERO) > 0 && curInc.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(SmartInsightItem.builder()
                    .id("INSIGHT-" + (insightCounter++))
                    .type("SURPLUS_ALERT")
                    .title("Monthly Net Savings Achieved")
                    .message("You have retained a net surplus of ₹" + net +
                            " (" + curRate + "% of income). Consider allocating a portion to your active savings goals.")
                    .severity("SUCCESS")
                    .actionable(true)
                    .category("Savings")
                    .build());
        }

        return SmartInsightResponse.builder()
                .period(current.toString())
                .hasEnoughData(true)
                .totalInsightsCount(insights.size())
                .insights(insights)
                .build();
    }

    // --- Backward Compatibility Implementations ---

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        OverviewSummaryResponse overview = getOverviewSummary(userId, null);
        int activeAccountsCount = accountRepository.findByUserIdAndDeletedAtIsNull(userId).size();

        return DashboardSummaryResponse.builder()
                .netWorth(overview.getNetWorth())
                .monthlyIncome(overview.getTotalIncome())
                .monthlyExpense(overview.getTotalExpenses())
                .monthlySavings(overview.getNetSavings())
                .savingsRate(overview.getSavingsRate())
                .activeAccountsCount(activeAccountsCount)
                .hasActiveBudget(overview.isHasActiveBudget())
                .budgetUtilization(overview.getBudgetUtilization())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashFlowResponse> getCashFlow(Long userId, int months) {
        List<IncomeVsExpenseResponse> raw = getIncomeVsExpenseComparison(userId, months);
        return raw.stream()
                .map(item -> CashFlowResponse.builder()
                        .monthYear(item.getMonthYear())
                        .label(item.getLabel())
                        .income(item.getIncome())
                        .expense(item.getExpense())
                        .netSavings(item.getNetSavings())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryExpenseResponse> getCategoryExpenses(Long userId) {
        return getCategoryExpenses(userId, null);
    }

    // --- Helper Methods ---

    private double calculateSavingsRate(BigDecimal income, BigDecimal savings) {
        if (income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return savings.multiply(new BigDecimal("100"))
                .divide(income, 2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }

    private double calculatePercentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0 || part == null) {
            return 0.0;
        }
        return part.multiply(new BigDecimal("100"))
                .divide(total, 2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }

    private double calculateChangePercentage(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) return 0.0;
            return 100.0;
        }
        if (current == null) current = BigDecimal.ZERO;

        BigDecimal diff = current.subtract(previous);
        return diff.multiply(new BigDecimal("100"))
                .divide(previous.abs(), 2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }

    private boolean isNeedsCategory(String nameLower) {
        return NEEDS_KEYWORDS.stream().anyMatch(nameLower::contains);
    }

    private boolean isSavingsCategory(String nameLower) {
        return SAVINGS_KEYWORDS.stream().anyMatch(nameLower::contains);
    }

    private double roundToOneDecimal(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_EVEN).doubleValue();
    }
}
