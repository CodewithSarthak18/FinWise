package com.finwise.service;

import com.finwise.dto.whatif.WhatIfPresetScenario;
import com.finwise.dto.whatif.WhatIfSimulationRequest;
import com.finwise.dto.whatif.WhatIfSimulationResponse;
import com.finwise.entity.Category;
import com.finwise.entity.CategoryType;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.service.impl.WhatIfCalculatorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("What-If Savings Calculator Unit Tests")
class WhatIfCalculatorTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WhatIfCalculatorServiceImpl whatIfCalculatorService;

    private Long userId;
    private Category diningCategory;

    @BeforeEach
    void setUp() {
        userId = 200L;
        diningCategory = Category.builder()
                .id(10L)
                .name("Food & Dining Out")
                .type(CategoryType.EXPENSE)
                .system(true)
                .build();
    }

    @ParameterizedTest
    @CsvSource({
        "5.00,  15000.00, 14250.00, 750.00,  9000.00",
        "10.00, 15000.00, 13500.00, 1500.00, 18000.00",
        "15.00, 15000.00, 12750.00, 2250.00, 27000.00",
        "20.00, 15000.00, 12000.00, 3000.00, 36000.00",
        "12.50, 15000.00, 13125.00, 1875.00, 22500.00"
    })
    @DisplayName("Should accurately calculate reduced spending, monthly savings and yearly savings")
    void testReductionCalculations(
            String reductionPctStr,
            String currentSpendStr,
            String expectedReducedStr,
            String expectedMonthlySavingsStr,
            String expectedYearlySavingsStr) {

        when(categoryRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(diningCategory));

        when(transactionRepository.sumExpenseByCategoryIdAndDateRange(eq(userId), eq(10L), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal(currentSpendStr));

        WhatIfSimulationRequest request = WhatIfSimulationRequest.builder()
                .categoryId(10L)
                .reductionPercentage(new BigDecimal(reductionPctStr))
                .build();

        WhatIfSimulationResponse response = whatIfCalculatorService.simulateSavings(userId, request);

        assertThat(response.getCategoryId()).isEqualTo(10L);
        assertThat(response.getCategoryName()).isEqualTo("Food & Dining Out");
        assertThat(response.getCurrentMonthlySpending()).isEqualByComparingTo(new BigDecimal(currentSpendStr));
        assertThat(response.getReducedMonthlySpending()).isEqualByComparingTo(new BigDecimal(expectedReducedStr));
        assertThat(response.getMonthlySavings()).isEqualByComparingTo(new BigDecimal(expectedMonthlySavingsStr));
        assertThat(response.getEstimatedYearlySavings()).isEqualByComparingTo(new BigDecimal(expectedYearlySavingsStr));

        // Invariant: Calculations must not modify real transaction records
        verify(transactionRepository, never()).save(any(com.finwise.entity.Transaction.class));
        verify(transactionRepository, never()).delete(any(com.finwise.entity.Transaction.class));
    }

    @Test
    @DisplayName("Should provide preset benchmark scenarios for 5%, 10%, 15%, 20%, 25%")
    void testPresetBenchmarks() {
        when(categoryRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(diningCategory));

        when(transactionRepository.sumExpenseByCategoryIdAndDateRange(eq(userId), eq(10L), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("20000.00"));

        WhatIfSimulationRequest request = WhatIfSimulationRequest.builder()
                .categoryId(10L)
                .reductionPercentage(new BigDecimal("10.00"))
                .build();

        WhatIfSimulationResponse response = whatIfCalculatorService.simulateSavings(userId, request);

        assertThat(response.getScenarios()).hasSize(5);

        WhatIfPresetScenario tenPercent = response.getScenarios().stream()
                .filter(s -> s.getPercentage().compareTo(new BigDecimal("10.00")) == 0)
                .findFirst().orElseThrow();

        assertThat(tenPercent.getMonthlySavings()).isEqualByComparingTo(new BigDecimal("2000.00")); // 20,000 * 10%
        assertThat(tenPercent.getEstimatedYearlySavings()).isEqualByComparingTo(new BigDecimal("24000.00")); // 2,000 * 12
        assertThat(tenPercent.getReducedMonthlySpending()).isEqualByComparingTo(new BigDecimal("18000.00"));

        // Notice statement requirement
        assertThat(response.getNotice()).contains("NOT been modified");
    }
}
