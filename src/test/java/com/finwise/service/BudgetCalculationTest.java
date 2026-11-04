package com.finwise.service;

import com.finwise.dto.budget.BudgetCategoryRequest;
import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.budget.BudgetResponse;
import com.finwise.entity.*;
import com.finwise.exception.DuplicateResourceException;
import com.finwise.mapper.BudgetMapper;
import com.finwise.repository.BudgetRepository;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.repository.UserRepository;
import com.finwise.service.impl.BudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetCalculationTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private BudgetMapper budgetMapper = new BudgetMapper();

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("user@finwise.in").build();
        testCategory = Category.builder().id(10L).name("Dining").type(CategoryType.EXPENSE).colorCode("#F97316").build();

        testBudget = Budget.builder()
                .id(100L)
                .user(testUser)
                .name("Monthly Budget")
                .periodType(BudgetPeriodType.MONTHLY)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .totalBudgetAmount(new BigDecimal("10000.00"))
                .notificationThreshold(new BigDecimal("80.00"))
                .active(true)
                .build();

        BudgetCategory bcat = BudgetCategory.builder()
                .budget(testBudget)
                .category(testCategory)
                .allocatedAmount(new BigDecimal("10000.00"))
                .build();

        testBudget.addBudgetCategory(bcat);
    }

    @Test
    @DisplayName("Smart Alert 50% Usage -> Status NORMAL")
    void verifyNormalProgressAlert() {
        // ₹5,000 spent out of ₹10,000 = 50.0%
        when(budgetRepository.findActiveBudgetForDate(eq(1L), any())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.sumExpenseByCategoryIdAndDateRange(eq(1L), eq(10L), any(), any()))
                .thenReturn(new BigDecimal("5000.00"));

        BudgetResponse response = budgetService.getActiveBudget(1L);

        assertThat(response).isNotNull();
        assertThat(response.getUtilizationPercentage()).isEqualTo(50.0);
        assertThat(response.getAlertStatus()).isEqualTo(BudgetAlertStatus.NORMAL);
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("Smart Alert 80% Usage -> Status WARNING")
    void verifyWarningAlert() {
        // ₹8,000 spent out of ₹10,000 = 80.0%
        when(budgetRepository.findActiveBudgetForDate(eq(1L), any())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.sumExpenseByCategoryIdAndDateRange(eq(1L), eq(10L), any(), any()))
                .thenReturn(new BigDecimal("8000.00"));

        BudgetResponse response = budgetService.getActiveBudget(1L);

        assertThat(response.getUtilizationPercentage()).isEqualTo(80.0);
        assertThat(response.getAlertStatus()).isEqualTo(BudgetAlertStatus.WARNING);
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("Smart Alert 100% Usage -> Status EXCEEDED")
    void verifyBudgetExceededAlert() {
        // ₹10,000 spent out of ₹10,000 = 100.0%
        when(budgetRepository.findActiveBudgetForDate(eq(1L), any())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.sumExpenseByCategoryIdAndDateRange(eq(1L), eq(10L), any(), any()))
                .thenReturn(new BigDecimal("10000.00"));

        BudgetResponse response = budgetService.getActiveBudget(1L);

        assertThat(response.getUtilizationPercentage()).isEqualTo(100.0);
        assertThat(response.getAlertStatus()).isEqualTo(BudgetAlertStatus.EXCEEDED);
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Smart Alert 120% Usage -> Status OVERSPENDING")
    void verifyOverspendingAlert() {
        // ₹12,000 spent out of ₹10,000 = 120.0%
        when(budgetRepository.findActiveBudgetForDate(eq(1L), any())).thenReturn(Optional.of(testBudget));
        when(transactionRepository.sumExpenseByCategoryIdAndDateRange(eq(1L), eq(10L), any(), any()))
                .thenReturn(new BigDecimal("12000.00"));

        BudgetResponse response = budgetService.getActiveBudget(1L);

        assertThat(response.getUtilizationPercentage()).isEqualTo(120.0);
        assertThat(response.getAlertStatus()).isEqualTo(BudgetAlertStatus.OVERSPENDING);
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("-2000.00");
    }

    @Test
    @DisplayName("Prevent duplicate budget creation for overlapping active period")
    void verifyDuplicateBudgetPrevention() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserIdOrderByStartDateDesc(1L)).thenReturn(List.of(testBudget));

        BudgetRequest overlappingRequest = BudgetRequest.builder()
                .name("Duplicate November Budget")
                .periodType(BudgetPeriodType.MONTHLY)
                .startDate(LocalDate.of(2026, 11, 15))
                .endDate(LocalDate.of(2026, 12, 15))
                .totalBudgetAmount(new BigDecimal("15000.00"))
                .build();

        assertThatThrownBy(() -> budgetService.createBudget(overlappingRequest, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("overlapping period");
    }
}
