package com.finwise.service.impl;

import com.finwise.dto.budget.BudgetCategoryResponse;
import com.finwise.dto.budget.BudgetResponse;
import com.finwise.dto.report.*;
import com.finwise.entity.*;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.repository.*;
import com.finwise.service.BudgetService;
import com.finwise.service.ReportService;
import com.finwise.service.report.CsvReportGenerator;
import com.finwise.service.report.PdfReportGenerator;
import com.finwise.util.IndianCurrencyFormatter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;
    private final SavingsGoalRepository savingsGoalRepository;
    private final GoalContributionRepository goalContributionRepository;

    private final PdfReportGenerator pdfReportGenerator;
    private final CsvReportGenerator csvReportGenerator;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Set<String> NEEDS_KEYWORDS = Set.of(
            "rent", "housing", "grocer", "provis", "utilit", "bill", "electr", "water",
            "transport", "fuel", "petrol", "diesel", "health", "medic", "doctor", "hospital",
            "educat", "book", "school", "college", "tuition", "insuran", "emi", "loan",
            "internet", "broadband", "wifi", "maid", "maintenance"
    );

    private static final Set<String> SAVINGS_KEYWORDS = Set.of(
            "invest", "sip", "mutual fund", "stock", "deposit", "gold", "epf", "ppf", "nps", "crypto", "saving"
    );

    // ==========================================
    // 1. MONTHLY FINANCIAL REPORT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public MonthlyReportData generateMonthlyReport(Long userId, Integer year, Integer month) {
        User user = getUser(userId);
        YearMonth targetYm = getYearMonth(year, month);
        LocalDate startLocalDate = targetYm.atDay(1);
        LocalDate endLocalDate = targetYm.atEndOfMonth();

        Instant startInstant = startLocalDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant endInstant = endLocalDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant().minusNanos(1);

        ReportMetadata metadata = buildMetadata(
                ReportType.MONTHLY,
                "Monthly Financial Report - " + targetYm.getMonth().name() + " " + targetYm.getYear(),
                user,
                startLocalDate,
                endLocalDate
        );

        BigDecimal income = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.INCOME, startInstant, endInstant));
        BigDecimal grossExpense = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, startInstant, endInstant));
        BigDecimal refund = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.REFUND, startInstant, endInstant));
        BigDecimal expense = grossExpense.subtract(refund).max(BigDecimal.ZERO);
        BigDecimal netSavings = income.subtract(expense);
        double savingsRate = calculateSavingsRate(income, netSavings);
        BigDecimal netWorth = getZeroIfNull(accountRepository.calculateTotalNetWorth(userId));
        long txCount = transactionRepository.countByUserIdAndTransactionDateBetween(userId, startInstant, endInstant);

        // Category breakdown
        List<ReportCategorySummary> categories = getDetailedCategoryExpenseSummaries(userId, startInstant, endInstant, expense);

        // Transactions list
        List<ReportTransactionItem> transactions = getItemizedTransactions(userId, startInstant, endInstant);

        // Active Budget for month
        BudgetResponse activeBudget = budgetService.getActiveBudget(userId);
        boolean hasBudget = activeBudget != null;

        return MonthlyReportData.builder()
                .metadata(metadata)
                .period(targetYm.getMonth().name() + " " + targetYm.getYear())
                .totalIncome(income)
                .formattedTotalIncome(IndianCurrencyFormatter.formatINR(income))
                .totalExpenses(expense)
                .formattedTotalExpenses(IndianCurrencyFormatter.formatINR(expense))
                .netSavings(netSavings)
                .formattedNetSavings(IndianCurrencyFormatter.formatINR(netSavings))
                .savingsRate(savingsRate)
                .formattedSavingsRate(IndianCurrencyFormatter.formatPercentage(savingsRate))
                .netWorth(netWorth)
                .formattedNetWorth(IndianCurrencyFormatter.formatINR(netWorth))
                .transactionCount(txCount)
                .hasBudget(hasBudget)
                .budgetName(hasBudget ? activeBudget.getName() : "N/A")
                .budgetAmount(hasBudget ? activeBudget.getTotalBudgetAmount() : BigDecimal.ZERO)
                .formattedBudgetAmount(hasBudget ? IndianCurrencyFormatter.formatINR(activeBudget.getTotalBudgetAmount()) : "Rs. 0.00")
                .budgetSpent(hasBudget ? activeBudget.getTotalSpentAmount() : BigDecimal.ZERO)
                .formattedBudgetSpent(hasBudget ? IndianCurrencyFormatter.formatINR(activeBudget.getTotalSpentAmount()) : "Rs. 0.00")
                .budgetRemaining(hasBudget ? activeBudget.getRemainingAmount() : BigDecimal.ZERO)
                .formattedBudgetRemaining(hasBudget ? IndianCurrencyFormatter.formatINR(activeBudget.getRemainingAmount()) : "Rs. 0.00")
                .budgetUtilization(hasBudget ? activeBudget.getUtilizationPercentage() : 0.0)
                .formattedBudgetUtilization(hasBudget ? IndianCurrencyFormatter.formatPercentage(activeBudget.getUtilizationPercentage()) : "0.00%")
                .budgetStatus(hasBudget ? activeBudget.getAlertStatus().name() : "NO_BUDGET")
                .categoryBreakdown(categories)
                .transactions(transactions)
                .build();
    }

    // ==========================================
    // 2. ANNUAL FINANCIAL REPORT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public AnnualReportData generateAnnualReport(Long userId, Integer year) {
        User user = getUser(userId);
        int targetYear = (year != null) ? year : Year.now(DEFAULT_ZONE).getValue();
        if (targetYear < 2000 || targetYear > 2100) {
            throw new BadRequestException("Invalid year. Must be between 2000 and 2100.");
        }

        LocalDate startLocalDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endLocalDate = LocalDate.of(targetYear, 12, 31);
        Instant startInstant = startLocalDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant endInstant = endLocalDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant().minusNanos(1);

        ReportMetadata metadata = buildMetadata(
                ReportType.ANNUAL,
                "Annual Financial Report - Year " + targetYear,
                user,
                startLocalDate,
                endLocalDate
        );

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        List<AnnualReportData.AnnualMonthlyRow> monthlyRows = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(targetYear, m);
            Instant mStart = ym.atDay(1).atStartOfDay(DEFAULT_ZONE).toInstant();
            Instant mEnd = ym.atEndOfMonth().plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant().minusNanos(1);

            BigDecimal inc = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.INCOME, mStart, mEnd));
            BigDecimal grossExp = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, mStart, mEnd));
            BigDecimal ref = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.REFUND, mStart, mEnd));
            BigDecimal exp = grossExp.subtract(ref).max(BigDecimal.ZERO);
            BigDecimal net = inc.subtract(exp);
            double rate = calculateSavingsRate(inc, net);

            totalIncome = totalIncome.add(inc);
            totalExpenses = totalExpenses.add(exp);

            monthlyRows.add(AnnualReportData.AnnualMonthlyRow.builder()
                    .monthName(ym.getMonth().name().substring(0, 3))
                    .monthNumber(m)
                    .income(inc)
                    .formattedIncome(IndianCurrencyFormatter.formatINR(inc))
                    .expenses(exp)
                    .formattedExpenses(IndianCurrencyFormatter.formatINR(exp))
                    .netSavings(net)
                    .formattedNetSavings(IndianCurrencyFormatter.formatINR(net))
                    .savingsRate(rate)
                    .formattedSavingsRate(IndianCurrencyFormatter.formatPercentage(rate))
                    .build());
        }

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        double savingsRate = calculateSavingsRate(totalIncome, netSavings);
        BigDecimal avgMonthlyExpense = totalExpenses.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);
        BigDecimal avgMonthlyIncome = totalIncome.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);

        List<ReportCategorySummary> topCategories = getDetailedCategoryExpenseSummaries(userId, startInstant, endInstant, totalExpenses);
        if (topCategories.size() > 8) {
            topCategories = topCategories.subList(0, 8);
        }

        return AnnualReportData.builder()
                .metadata(metadata)
                .year(targetYear)
                .totalIncome(totalIncome)
                .formattedTotalIncome(IndianCurrencyFormatter.formatINR(totalIncome))
                .totalExpenses(totalExpenses)
                .formattedTotalExpenses(IndianCurrencyFormatter.formatINR(totalExpenses))
                .netSavings(netSavings)
                .formattedNetSavings(IndianCurrencyFormatter.formatINR(netSavings))
                .savingsRate(savingsRate)
                .formattedSavingsRate(IndianCurrencyFormatter.formatPercentage(savingsRate))
                .monthlyAverageIncome(avgMonthlyIncome)
                .formattedMonthlyAverageIncome(IndianCurrencyFormatter.formatINR(avgMonthlyIncome))
                .monthlyAverageExpense(avgMonthlyExpense)
                .formattedMonthlyAverageExpense(IndianCurrencyFormatter.formatINR(avgMonthlyExpense))
                .monthlyBreakdown(monthlyRows)
                .topExpenseCategories(topCategories)
                .build();
    }

    // ==========================================
    // 3. CATEGORY-WISE REPORT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public CategoryReportData generateCategoryReport(Long userId, LocalDate startDate, LocalDate endDate, Long categoryId) {
        User user = getUser(userId);
        validateDateRange(startDate, endDate);

        Instant startInstant = startDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant().minusNanos(1);

        ReportMetadata metadata = buildMetadata(
                ReportType.CATEGORY_WISE,
                "Category-Wise Financial Analysis",
                user,
                startDate,
                endDate
        );

        BigDecimal totalExpenses = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, startInstant, endInstant));
        BigDecimal totalIncome = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.INCOME, startInstant, endInstant));

        List<Object[]> rawCatList = transactionRepository.findDetailedCategoryBreakdownAllTypes(userId, startInstant, endInstant);
        List<ReportCategorySummary> summaries = new ArrayList<>();

        if (rawCatList != null) {
            for (Object[] row : rawCatList) {
                Long catId = ((Number) row[0]).longValue();
                if (categoryId != null && !catId.equals(categoryId)) {
                    continue; // filter by selected category if provided
                }

                String catName = (String) row[1];
                TransactionType type = (TransactionType) row[2];
                BigDecimal amount = (BigDecimal) row[3];
                long count = ((Number) row[4]).longValue();

                BigDecimal basis = type == TransactionType.INCOME ? totalIncome : totalExpenses;
                double pct = basis.signum() > 0 ? (amount.doubleValue() / basis.doubleValue()) * 100.0 : 0.0;
                BigDecimal avg = count > 0 ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO;

                summaries.add(ReportCategorySummary.builder()
                        .categoryName(catName)
                        .type(type.name())
                        .amount(amount)
                        .formattedAmount(IndianCurrencyFormatter.formatINR(amount))
                        .transactionCount(count)
                        .percentageOfTotal(pct)
                        .formattedPercentage(IndianCurrencyFormatter.formatPercentage(pct))
                        .averageAmount(avg)
                        .formattedAverage(IndianCurrencyFormatter.formatINR(avg))
                        .build());
            }
        }

        List<ReportTransactionItem> txList = getItemizedTransactions(userId, startInstant, endInstant);
        if (categoryId != null) {
            txList = txList.stream()
                    .filter(tx -> tx.getCategory() != null)
                    .collect(Collectors.toList());
        }

        return CategoryReportData.builder()
                .metadata(metadata)
                .totalExpenses(totalExpenses)
                .formattedTotalExpenses(IndianCurrencyFormatter.formatINR(totalExpenses))
                .totalIncome(totalIncome)
                .formattedTotalIncome(IndianCurrencyFormatter.formatINR(totalIncome))
                .categoryCount(summaries.size())
                .transactionCount(txList.size())
                .categorySummaries(summaries)
                .transactions(txList)
                .build();
    }

    // ==========================================
    // 4. INCOME AND EXPENSE SUMMARY
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public IncomeExpenseReportData generateIncomeExpenseReport(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = getUser(userId);
        validateDateRange(startDate, endDate);

        Instant startInstant = startDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant().minusNanos(1);

        ReportMetadata metadata = buildMetadata(
                ReportType.INCOME_EXPENSE,
                "Income & Expense Cash Flow Summary",
                user,
                startDate,
                endDate
        );

        BigDecimal income = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.INCOME, startInstant, endInstant));
        BigDecimal grossExpense = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, startInstant, endInstant));
        BigDecimal refund = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.REFUND, startInstant, endInstant));
        BigDecimal expense = grossExpense.subtract(refund).max(BigDecimal.ZERO);
        BigDecimal netFlow = income.subtract(expense);
        double savingsRate = calculateSavingsRate(income, netFlow);

        List<Transaction> txList = transactionRepository.findByUserIdAndDateRange(userId, startInstant, endInstant);

        // 50/30/20 Bucket calculations
        BigDecimal needs = BigDecimal.ZERO;
        BigDecimal savingsInvestments = BigDecimal.ZERO;
        BigDecimal wants = BigDecimal.ZERO;

        for (Transaction t : txList) {
            if (t.getTransactionType() == TransactionType.EXPENSE) {
                String catLower = t.getCategory().getName().toLowerCase();
                String descLower = t.getDescription() != null ? t.getDescription().toLowerCase() : "";

                boolean isNeed = NEEDS_KEYWORDS.stream().anyMatch(k -> catLower.contains(k) || descLower.contains(k));
                boolean isSavings = SAVINGS_KEYWORDS.stream().anyMatch(k -> catLower.contains(k) || descLower.contains(k));

                if (isSavings) {
                    savingsInvestments = savingsInvestments.add(t.getAmount());
                } else if (isNeed) {
                    needs = needs.add(t.getAmount());
                } else {
                    wants = wants.add(t.getAmount());
                }
            }
        }

        BigDecimal totalExp = expense.signum() > 0 ? expense : BigDecimal.ONE;
        double needsPct = (needs.doubleValue() / totalExp.doubleValue()) * 100.0;
        double wantsPct = (wants.doubleValue() / totalExp.doubleValue()) * 100.0;
        double savingsPct = (savingsInvestments.doubleValue() / totalExp.doubleValue()) * 100.0;

        List<ReportCategorySummary> incSummaries = getCategorySummariesByType(userId, startInstant, endInstant, TransactionType.INCOME, income);
        List<ReportCategorySummary> expSummaries = getCategorySummariesByType(userId, startInstant, endInstant, TransactionType.EXPENSE, expense);

        List<ReportTransactionItem> items = txList.stream()
                .map(this::mapTransactionItem)
                .collect(Collectors.toList());

        return IncomeExpenseReportData.builder()
                .metadata(metadata)
                .totalIncome(income)
                .formattedTotalIncome(IndianCurrencyFormatter.formatINR(income))
                .totalExpenses(expense)
                .formattedTotalExpenses(IndianCurrencyFormatter.formatINR(expense))
                .netCashFlow(netFlow)
                .formattedNetCashFlow(IndianCurrencyFormatter.formatINR(netFlow))
                .savingsRate(savingsRate)
                .formattedSavingsRate(IndianCurrencyFormatter.formatPercentage(savingsRate))
                .needsAmount(needs)
                .formattedNeedsAmount(IndianCurrencyFormatter.formatINR(needs))
                .needsPercentage(needsPct)
                .formattedNeedsPercentage(IndianCurrencyFormatter.formatPercentage(needsPct))
                .wantsAmount(wants)
                .formattedWantsAmount(IndianCurrencyFormatter.formatINR(wants))
                .wantsPercentage(wantsPct)
                .formattedWantsPercentage(IndianCurrencyFormatter.formatPercentage(wantsPct))
                .savingsInvestmentsAmount(savingsInvestments)
                .formattedSavingsInvestmentsAmount(IndianCurrencyFormatter.formatINR(savingsInvestments))
                .savingsInvestmentsPercentage(savingsPct)
                .formattedSavingsInvestmentsPercentage(IndianCurrencyFormatter.formatPercentage(savingsPct))
                .incomeCategories(incSummaries)
                .expenseCategories(expSummaries)
                .transactions(items)
                .build();
    }

    // ==========================================
    // 5. SAVINGS SUMMARY REPORT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public SavingsReportData generateSavingsReport(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = getUser(userId);
        validateDateRange(startDate, endDate);

        Instant startInstant = startDate.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant().minusNanos(1);

        ReportMetadata metadata = buildMetadata(
                ReportType.SAVINGS,
                "Savings & Wealth Goals Performance",
                user,
                startDate,
                endDate
        );

        BigDecimal income = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.INCOME, startInstant, endInstant));
        BigDecimal grossExpense = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.EXPENSE, startInstant, endInstant));
        BigDecimal refund = getZeroIfNull(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(userId, TransactionType.REFUND, startInstant, endInstant));
        BigDecimal expense = grossExpense.subtract(refund).max(BigDecimal.ZERO);
        BigDecimal periodSavings = income.subtract(expense);
        double periodSavingsRate = calculateSavingsRate(income, periodSavings);

        List<SavingsGoal> userGoals = savingsGoalRepository.findByUserIdOrderByTargetDateAsc(userId);
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;

        List<SavingsReportData.SavingsGoalReportItem> goalItems = new ArrayList<>();
        for (SavingsGoal g : userGoals) {
            totalTarget = totalTarget.add(g.getTargetAmount());
            totalCurrent = totalCurrent.add(g.getCurrentAmount());

            BigDecimal rem = g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO);
            double pct = g.getTargetAmount().signum() > 0 ?
                    (g.getCurrentAmount().doubleValue() / g.getTargetAmount().doubleValue()) * 100.0 : 0.0;

            goalItems.add(SavingsReportData.SavingsGoalReportItem.builder()
                    .title(g.getTitle())
                    .targetAmount(g.getTargetAmount())
                    .formattedTargetAmount(IndianCurrencyFormatter.formatINR(g.getTargetAmount()))
                    .currentAmount(g.getCurrentAmount())
                    .formattedCurrentAmount(IndianCurrencyFormatter.formatINR(g.getCurrentAmount()))
                    .remainingAmount(rem)
                    .formattedRemainingAmount(IndianCurrencyFormatter.formatINR(rem))
                    .progressPercentage(pct)
                    .formattedProgressPercentage(IndianCurrencyFormatter.formatPercentage(pct))
                    .targetDate(g.getTargetDate() != null ? g.getTargetDate().format(DISPLAY_DATE_FMT) : "-")
                    .priority(g.getPriority() != null ? g.getPriority().name() : "MEDIUM")
                    .status(g.getStatus() != null ? g.getStatus().name() : "IN_PROGRESS")
                    .build());
        }

        BigDecimal totalRemaining = totalTarget.subtract(totalCurrent).max(BigDecimal.ZERO);
        double overallGoalProgress = totalTarget.signum() > 0 ?
                (totalCurrent.doubleValue() / totalTarget.doubleValue()) * 100.0 : 0.0;

        // Period contributions
        List<GoalContribution> contributions = goalContributionRepository.findByUserIdAndContributionDateBetween(userId, startInstant, endInstant);
        List<SavingsReportData.GoalContributionReportItem> contribItems = contributions.stream()
                .map(c -> SavingsReportData.GoalContributionReportItem.builder()
                        .contributionDate(c.getContributionDate().atZone(DEFAULT_ZONE).format(DISPLAY_DATE_FMT))
                        .goalTitle(c.getSavingsGoal().getTitle())
                        .accountName(c.getAccount().getAccountName())
                        .amount(c.getAmount())
                        .formattedAmount(IndianCurrencyFormatter.formatINR(c.getAmount()))
                        .note(c.getNote())
                        .build())
                .collect(Collectors.toList());

        return SavingsReportData.builder()
                .metadata(metadata)
                .netSavingsInPeriod(periodSavings)
                .formattedNetSavingsInPeriod(IndianCurrencyFormatter.formatINR(periodSavings))
                .savingsRateInPeriod(periodSavingsRate)
                .formattedSavingsRateInPeriod(IndianCurrencyFormatter.formatPercentage(periodSavingsRate))
                .totalGoalTargetAmount(totalTarget)
                .formattedTotalGoalTargetAmount(IndianCurrencyFormatter.formatINR(totalTarget))
                .totalGoalCurrentAmount(totalCurrent)
                .formattedTotalGoalCurrentAmount(IndianCurrencyFormatter.formatINR(totalCurrent))
                .totalGoalRemainingAmount(totalRemaining)
                .formattedTotalGoalRemainingAmount(IndianCurrencyFormatter.formatINR(totalRemaining))
                .overallGoalProgress(overallGoalProgress)
                .formattedOverallGoalProgress(IndianCurrencyFormatter.formatPercentage(overallGoalProgress))
                .goals(goalItems)
                .contributions(contribItems)
                .build();
    }

    // ==========================================
    // 6. BUDGET PERFORMANCE REPORT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public BudgetPerformanceReportData generateBudgetPerformanceReport(Long userId, Long budgetId, Integer year, Integer month) {
        User user = getUser(userId);
        Budget budget = null;

        if (budgetId != null) {
            budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", budgetId));
        } else {
            YearMonth ym = getYearMonth(year, month);
            LocalDate refDate = ym.atDay(15);
            budget = budgetRepository.findActiveBudgetForDate(userId, refDate).orElse(null);
            if (budget == null) {
                List<Budget> userBudgets = budgetRepository.findByUserIdOrderByStartDateDesc(userId);
                if (!userBudgets.isEmpty()) {
                    budget = userBudgets.get(0);
                }
            }
        }

        if (budget == null) {
            LocalDate start = (year != null && month != null) ? YearMonth.of(year, month).atDay(1) : LocalDate.now(DEFAULT_ZONE).withDayOfMonth(1);
            LocalDate end = start.plusMonths(1).minusDays(1);
            ReportMetadata meta = buildMetadata(ReportType.BUDGET_PERFORMANCE, "Budget Performance Report", user, start, end);
            return BudgetPerformanceReportData.builder()
                    .metadata(meta)
                    .hasBudget(false)
                    .budgetName("No Active Budget")
                    .totalBudgetAmount(BigDecimal.ZERO)
                    .formattedTotalBudgetAmount("Rs. 0.00")
                    .totalSpentAmount(BigDecimal.ZERO)
                    .formattedTotalSpentAmount("Rs. 0.00")
                    .remainingAmount(BigDecimal.ZERO)
                    .formattedRemainingAmount("Rs. 0.00")
                    .utilizationPercentage(0.0)
                    .formattedUtilizationPercentage("0.00%")
                    .alertStatus("NO_BUDGET")
                    .build();
        }

        BudgetResponse response = budgetService.getBudgetById(budget.getId(), userId);

        ReportMetadata metadata = buildMetadata(
                ReportType.BUDGET_PERFORMANCE,
                "Budget Performance Report - " + budget.getName(),
                user,
                budget.getStartDate(),
                budget.getEndDate()
        );

        List<BudgetPerformanceReportData.BudgetCategoryPerformanceItem> catPerf = new ArrayList<>();
        List<String> observations = new ArrayList<>();

        if (response.getCategories() != null) {
            for (BudgetCategoryResponse cat : response.getCategories()) {
                boolean overBudget = cat.getRemainingAmount().signum() < 0 || cat.getUtilizationPercentage() > 100.0;
                String status = overBudget ? "OVER_BUDGET" : (cat.getUtilizationPercentage() >= 80.0 ? "WARNING" : "NORMAL");

                catPerf.add(BudgetPerformanceReportData.BudgetCategoryPerformanceItem.builder()
                        .categoryName(cat.getCategoryName())
                        .allocatedAmount(cat.getAllocatedAmount())
                        .formattedAllocatedAmount(IndianCurrencyFormatter.formatINR(cat.getAllocatedAmount()))
                        .spentAmount(cat.getSpentAmount())
                        .formattedSpentAmount(IndianCurrencyFormatter.formatINR(cat.getSpentAmount()))
                        .remainingAmount(cat.getRemainingAmount())
                        .formattedRemainingAmount(IndianCurrencyFormatter.formatINR(cat.getRemainingAmount()))
                        .utilizationPercentage(cat.getUtilizationPercentage())
                        .formattedUtilizationPercentage(IndianCurrencyFormatter.formatPercentage(cat.getUtilizationPercentage()))
                        .status(status)
                        .overBudget(overBudget)
                        .build());

                if (overBudget) {
                    observations.add(String.format("Category '%s' is over budget by %s (utilization: %.1f%%).",
                            cat.getCategoryName(),
                            IndianCurrencyFormatter.formatINR(cat.getRemainingAmount().abs()),
                            cat.getUtilizationPercentage()));
                } else if (cat.getUtilizationPercentage() >= 80.0) {
                    observations.add(String.format("Category '%s' has reached %.1f%% of its allocated limit.",
                            cat.getCategoryName(),
                            cat.getUtilizationPercentage()));
                }
            }
        }

        if (observations.isEmpty()) {
            observations.add("All category spending is well within planned allocation limits.");
        }

        return BudgetPerformanceReportData.builder()
                .metadata(metadata)
                .budgetId(budget.getId())
                .budgetName(budget.getName())
                .periodType(budget.getPeriodType().name())
                .startDate(budget.getStartDate().format(DISPLAY_DATE_FMT))
                .endDate(budget.getEndDate().format(DISPLAY_DATE_FMT))
                .totalBudgetAmount(response.getTotalBudgetAmount())
                .formattedTotalBudgetAmount(IndianCurrencyFormatter.formatINR(response.getTotalBudgetAmount()))
                .totalSpentAmount(response.getTotalSpentAmount())
                .formattedTotalSpentAmount(IndianCurrencyFormatter.formatINR(response.getTotalSpentAmount()))
                .remainingAmount(response.getRemainingAmount())
                .formattedRemainingAmount(IndianCurrencyFormatter.formatINR(response.getRemainingAmount()))
                .utilizationPercentage(response.getUtilizationPercentage())
                .formattedUtilizationPercentage(IndianCurrencyFormatter.formatPercentage(response.getUtilizationPercentage()))
                .alertStatus(response.getAlertStatus().name())
                .notificationThreshold(response.getNotificationThreshold())
                .hasBudget(true)
                .categoryPerformance(catPerf)
                .observations(observations)
                .build();
    }

    // ==========================================
    // EXPORT DISPATCHER & FILENAME GENERATOR
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public byte[] exportReport(ReportType type, ExportFormat format, Long userId, Map<String, Object> params) {
        if (format == null) format = ExportFormat.PDF;

        switch (type) {
            case MONTHLY -> {
                Integer year = (Integer) params.get("year");
                Integer month = (Integer) params.get("month");
                MonthlyReportData data = generateMonthlyReport(userId, year, month);
                return format == ExportFormat.CSV ?
                        csvReportGenerator.generateMonthlyReport(data) :
                        pdfReportGenerator.generateMonthlyReport(data);
            }
            case ANNUAL -> {
                Integer year = (Integer) params.get("year");
                AnnualReportData data = generateAnnualReport(userId, year);
                return format == ExportFormat.CSV ?
                        csvReportGenerator.generateAnnualReport(data) :
                        pdfReportGenerator.generateAnnualReport(data);
            }
            case CATEGORY_WISE -> {
                LocalDate start = (LocalDate) params.get("startDate");
                LocalDate end = (LocalDate) params.get("endDate");
                Long categoryId = (Long) params.get("categoryId");
                CategoryReportData data = generateCategoryReport(userId, start, end, categoryId);
                return format == ExportFormat.CSV ?
                        csvReportGenerator.generateCategoryReport(data) :
                        pdfReportGenerator.generateCategoryReport(data);
            }
            case INCOME_EXPENSE -> {
                LocalDate start = (LocalDate) params.get("startDate");
                LocalDate end = (LocalDate) params.get("endDate");
                IncomeExpenseReportData data = generateIncomeExpenseReport(userId, start, end);
                return format == ExportFormat.CSV ?
                        csvReportGenerator.generateIncomeExpenseReport(data) :
                        pdfReportGenerator.generateIncomeExpenseReport(data);
            }
            case SAVINGS -> {
                LocalDate start = (LocalDate) params.get("startDate");
                LocalDate end = (LocalDate) params.get("endDate");
                SavingsReportData data = generateSavingsReport(userId, start, end);
                return format == ExportFormat.CSV ?
                        csvReportGenerator.generateSavingsReport(data) :
                        pdfReportGenerator.generateSavingsReport(data);
            }
            case BUDGET_PERFORMANCE -> {
                Long budgetId = (Long) params.get("budgetId");
                Integer year = (Integer) params.get("year");
                Integer month = (Integer) params.get("month");
                BudgetPerformanceReportData data = generateBudgetPerformanceReport(userId, budgetId, year, month);
                return format == ExportFormat.CSV ?
                        csvReportGenerator.generateBudgetPerformanceReport(data) :
                        pdfReportGenerator.generateBudgetPerformanceReport(data);
            }
            default -> throw new BadRequestException("Unsupported report type: " + type);
        }
    }

    @Override
    public String getFilename(ReportType type, ExportFormat format, Map<String, Object> params) {
        String ext = format == ExportFormat.CSV ? "csv" : "pdf";
        String timePrefix = "finwise-" + type.name().toLowerCase().replace('_', '-');

        if (type == ReportType.MONTHLY) {
            Integer year = (Integer) params.getOrDefault("year", LocalDate.now(DEFAULT_ZONE).getYear());
            Integer month = (Integer) params.getOrDefault("month", LocalDate.now(DEFAULT_ZONE).getMonthValue());
            return String.format("%s-%d-%02d.%s", timePrefix, year, month, ext);
        } else if (type == ReportType.ANNUAL) {
            Integer year = (Integer) params.getOrDefault("year", LocalDate.now(DEFAULT_ZONE).getYear());
            return String.format("%s-%d.%s", timePrefix, year, ext);
        } else {
            LocalDate start = (LocalDate) params.getOrDefault("startDate", LocalDate.now(DEFAULT_ZONE).withDayOfMonth(1));
            LocalDate end = (LocalDate) params.getOrDefault("endDate", LocalDate.now(DEFAULT_ZONE));
            return String.format("%s-%s_to_%s.%s", timePrefix, start.format(ISO_DATE_FMT), end.format(ISO_DATE_FMT), ext);
        }
    }

    // ==========================================
    // PRIVATE DOMAIN UTILITIES
    // ==========================================

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private YearMonth getYearMonth(Integer year, Integer month) {
        if (year != null && month != null) {
            if (year < 2000 || year > 2100) {
                throw new BadRequestException("Invalid year. Must be between 2000 and 2100.");
            }
            if (month < 1 || month > 12) {
                throw new BadRequestException("Invalid month. Must be between 1 and 12.");
            }
            return YearMonth.of(year, month);
        }
        return YearMonth.now(DEFAULT_ZONE);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Both startDate and endDate are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("startDate cannot be after endDate.");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 1826) {
            throw new BadRequestException("Date range cannot exceed 5 years.");
        }
    }

    private ReportMetadata buildMetadata(ReportType type, String title, User user, LocalDate start, LocalDate end) {
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                          (user.getLastName() != null ? user.getLastName() : "").trim();
        if (fullName.isBlank()) fullName = "FinWise User";

        Instant now = Instant.now();
        String genAt = now.atZone(DEFAULT_ZONE).format(DISPLAY_DT_FMT);
        String rangeText = start.format(DISPLAY_DATE_FMT) + " to " + end.format(DISPLAY_DATE_FMT);

        return ReportMetadata.builder()
                .title(title)
                .reportType(type)
                .userName(fullName)
                .userEmail(user.getEmail())
                .generatedAt(genAt)
                .generatedTimestamp(now)
                .dateRangeText(rangeText)
                .startDate(start.format(ISO_DATE_FMT))
                .endDate(end.format(ISO_DATE_FMT))
                .currencyCode("INR")
                .currencySymbol("Rs.")
                .build();
    }

    private double calculateSavingsRate(BigDecimal income, BigDecimal netSavings) {
        if (income == null || income.signum() <= 0) {
            return 0.0;
        }
        if (netSavings == null || netSavings.signum() <= 0) {
            return 0.0;
        }
        return (netSavings.doubleValue() / income.doubleValue()) * 100.0;
    }

    private BigDecimal getZeroIfNull(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private List<ReportCategorySummary> getDetailedCategoryExpenseSummaries(Long userId, Instant start, Instant end, BigDecimal totalExpense) {
        List<Object[]> raw = transactionRepository.findDetailedCategoryExpenseBreakdown(userId, start, end);
        List<ReportCategorySummary> list = new ArrayList<>();
        if (raw != null) {
            for (Object[] row : raw) {
                String name = (String) row[1];
                BigDecimal amount = (BigDecimal) row[4];
                long count = ((Number) row[5]).longValue();
                double pct = totalExpense.signum() > 0 ? (amount.doubleValue() / totalExpense.doubleValue()) * 100.0 : 0.0;
                BigDecimal avg = count > 0 ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO;

                list.add(ReportCategorySummary.builder()
                        .categoryName(name)
                        .type("EXPENSE")
                        .amount(amount)
                        .formattedAmount(IndianCurrencyFormatter.formatINR(amount))
                        .transactionCount(count)
                        .percentageOfTotal(pct)
                        .formattedPercentage(IndianCurrencyFormatter.formatPercentage(pct))
                        .averageAmount(avg)
                        .formattedAverage(IndianCurrencyFormatter.formatINR(avg))
                        .build());
            }
        }
        return list;
    }

    private List<ReportCategorySummary> getCategorySummariesByType(Long userId, Instant start, Instant end, TransactionType type, BigDecimal totalAmount) {
        List<Object[]> raw = transactionRepository.findDetailedCategoryBreakdownAllTypes(userId, start, end);
        List<ReportCategorySummary> list = new ArrayList<>();
        if (raw != null) {
            for (Object[] row : raw) {
                TransactionType rowType = (TransactionType) row[2];
                if (rowType == type) {
                    String name = (String) row[1];
                    BigDecimal amount = (BigDecimal) row[3];
                    long count = ((Number) row[4]).longValue();
                    double pct = totalAmount.signum() > 0 ? (amount.doubleValue() / totalAmount.doubleValue()) * 100.0 : 0.0;
                    BigDecimal avg = count > 0 ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO;

                    list.add(ReportCategorySummary.builder()
                            .categoryName(name)
                            .type(type.name())
                            .amount(amount)
                            .formattedAmount(IndianCurrencyFormatter.formatINR(amount))
                            .transactionCount(count)
                            .percentageOfTotal(pct)
                            .formattedPercentage(IndianCurrencyFormatter.formatPercentage(pct))
                            .averageAmount(avg)
                            .formattedAverage(IndianCurrencyFormatter.formatINR(avg))
                            .build());
                }
            }
        }
        return list;
    }

    private List<ReportTransactionItem> getItemizedTransactions(Long userId, Instant start, Instant end) {
        List<Transaction> list = transactionRepository.findByUserIdAndDateRange(userId, start, end);
        return list.stream()
                .map(this::mapTransactionItem)
                .collect(Collectors.toList());
    }

    private ReportTransactionItem mapTransactionItem(Transaction t) {
        String dt = t.getTransactionDate().atZone(DEFAULT_ZONE).format(DISPLAY_DATE_FMT);
        String catName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
        String accName = t.getAccount() != null ? t.getAccount().getAccountName() : "Main Account";
        String mode = t.getPaymentMode() != null ? t.getPaymentMode().name() : "OTHER";

        return ReportTransactionItem.builder()
                .date(dt)
                .description(t.getDescription())
                .category(catName)
                .account(accName)
                .type(t.getTransactionType().name())
                .paymentMethod(mode)
                .amount(t.getAmount())
                .formattedAmount(IndianCurrencyFormatter.formatINR(t.getAmount()))
                .build();
    }
}
