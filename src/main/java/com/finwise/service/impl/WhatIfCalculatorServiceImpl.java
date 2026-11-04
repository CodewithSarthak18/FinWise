package com.finwise.service.impl;

import com.finwise.dto.whatif.CategorySpendingOption;
import com.finwise.dto.whatif.WhatIfPresetScenario;
import com.finwise.dto.whatif.WhatIfSimulationRequest;
import com.finwise.dto.whatif.WhatIfSimulationResponse;
import com.finwise.entity.Category;
import com.finwise.entity.TransactionType;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.exception.UnauthorizedException;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.service.WhatIfCalculatorService;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WhatIfCalculatorServiceImpl implements WhatIfCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(WhatIfCalculatorServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    private static final List<BigDecimal> PRESET_PERCENTAGES = List.of(
            new BigDecimal("5.00"),
            new BigDecimal("10.00"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            new BigDecimal("25.00")
    );

    @Override
    @Transactional(readOnly = true)
    public WhatIfSimulationResponse simulateSavings(Long userId, WhatIfSimulationRequest request) {
        log.debug("Running what-if simulation for user id {}, category id {}, reduction: {}%",
                userId, request.getCategoryId(), request.getReductionPercentage());

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        if (!category.isSystem() && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
            throw new UnauthorizedException("You do not have access to this category");
        }

        YearMonth targetMonth = parseMonth(request.getMonth());
        Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        BigDecimal currentSpending = transactionRepository.sumExpenseByCategoryIdAndDateRange(
                userId, category.getId(), start, end);
        if (currentSpending == null) {
            currentSpending = BigDecimal.ZERO;
        }

        // If current month has 0 spending, fallback to average spending over past 3 months
        if (currentSpending.compareTo(BigDecimal.ZERO) == 0) {
            Instant threeMonthsAgo = targetMonth.minusMonths(3).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            BigDecimal threeMonthTotal = transactionRepository.sumExpenseByCategoryIdAndDateRange(
                    userId, category.getId(), threeMonthsAgo, end);
            if (threeMonthTotal != null && threeMonthTotal.compareTo(BigDecimal.ZERO) > 0) {
                currentSpending = threeMonthTotal.divide(new BigDecimal("3"), 2, RoundingMode.HALF_EVEN);
            }
        }

        BigDecimal reductionPct = request.getReductionPercentage();
        BigDecimal monthlySavings = currentSpending.multiply(reductionPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        BigDecimal reducedSpending = currentSpending.subtract(monthlySavings);
        BigDecimal yearlySavings = monthlySavings.multiply(new BigDecimal("12")).setScale(2, RoundingMode.HALF_EVEN);

        // Precompute standard preset scenarios (5%, 10%, 15%, 20%, 25%)
        List<WhatIfPresetScenario> scenarios = new ArrayList<>();
        for (BigDecimal pct : PRESET_PERCENTAGES) {
            BigDecimal mSavings = currentSpending.multiply(pct)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
            BigDecimal rSpending = currentSpending.subtract(mSavings);
            BigDecimal ySavings = mSavings.multiply(new BigDecimal("12")).setScale(2, RoundingMode.HALF_EVEN);

            String label = switch (pct.intValue()) {
                case 5 -> "5% Trim";
                case 10 -> "10% Moderate";
                case 15 -> "15% Target";
                case 20 -> "20% High Impact";
                case 25 -> "25% Aggressive";
                default -> pct + "% Reduction";
            };

            scenarios.add(WhatIfPresetScenario.builder()
                    .percentage(pct)
                    .label(label)
                    .reducedMonthlySpending(rSpending)
                    .monthlySavings(mSavings)
                    .estimatedYearlySavings(ySavings)
                    .build());
        }

        String summary = String.format(
                "Reducing your '%s' spending by %s%% will save ₹%s each month, unlocking an estimated ₹%s in annual savings.",
                category.getName(), reductionPct.stripTrailingZeros().toPlainString(), monthlySavings, yearlySavings
        );

        return WhatIfSimulationResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .reductionPercentage(reductionPct)
                .currentMonthlySpending(currentSpending)
                .reducedMonthlySpending(reducedSpending)
                .monthlySavings(monthlySavings)
                .estimatedYearlySavings(yearlySavings)
                .currencyCode("INR")
                .summary(summary)
                .scenarios(scenarios)
                .notice("Simulated projection only. Real transaction records and account balances have NOT been modified.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySpendingOption> getSpendingCategories(Long userId, YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now(ZoneOffset.UTC);
        Instant start = targetMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);

        List<Object[]> rawData = transactionRepository.findDetailedCategoryExpenseBreakdown(userId, start, end);
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Object[] row : rawData) {
            BigDecimal amt = (BigDecimal) row[4];
            if (amt != null) totalExpenses = totalExpenses.add(amt);
        }

        List<CategorySpendingOption> options = new ArrayList<>();
        for (Object[] row : rawData) {
            Long catId = (Long) row[0];
            String name = (String) row[1];
            String color = (String) row[2];
            String icon = (String) row[3];
            BigDecimal amt = (BigDecimal) row[4];

            double pct = 0.0;
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0 && amt != null) {
                pct = amt.multiply(new BigDecimal("100"))
                        .divide(totalExpenses, 2, RoundingMode.HALF_EVEN)
                        .doubleValue();
            }

            options.add(CategorySpendingOption.builder()
                    .categoryId(catId)
                    .categoryName(name)
                    .colorCode(color)
                    .icon(icon)
                    .currentMonthlySpending(amt)
                    .percentageOfTotalExpenses(pct)
                    .build());
        }

        return options;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhatIfPresetScenario> getPresetScenarios(Long userId, Long categoryId, YearMonth month) {
        WhatIfSimulationRequest req = WhatIfSimulationRequest.builder()
                .categoryId(categoryId)
                .reductionPercentage(new BigDecimal("10.00"))
                .month(month != null ? month.toString() : null)
                .build();
        return simulateSavings(userId, req).getScenarios();
    }

    private YearMonth parseMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        try {
            return YearMonth.parse(monthStr.trim());
        } catch (Exception e) {
            log.warn("Invalid month string '{}', falling back to current month", monthStr);
            return YearMonth.now(ZoneOffset.UTC);
        }
    }
}
