package com.finwise.service;

import com.finwise.dto.subscription.DetectedSubscriptionResponse;
import com.finwise.dto.subscription.SubscriptionDetectionSummaryResponse;
import com.finwise.entity.*;
import com.finwise.repository.RecurringTransactionRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.service.impl.SubscriptionDetectorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Detector Unit Tests")
class SubscriptionDetectorTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringTransactionRepository recurringRepository;

    @Mock
    private RecurringTransactionService recurringTransactionService;

    @InjectMocks
    private SubscriptionDetectorServiceImpl subscriptionDetectorService;

    private Long userId;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        userId = 101L;

        testAccount = Account.builder()
                .id(1L)
                .accountName("HDFC Bank")
                .currencyCode("INR")
                .build();

        testCategory = Category.builder()
                .id(2L)
                .name("Entertainment & Subscriptions")
                .build();
    }

    @Test
    @DisplayName("Should detect monthly recurring subscription from recurring transaction patterns")
    void testDetectMonthlySubscription() {
        LocalDate baseDate = LocalDate.now().minusMonths(3);

        Transaction tx1 = Transaction.builder()
                .id(1L)
                .merchantName("Netflix India")
                .description("Netflix Monthly Subscription")
                .amount(new BigDecimal("649.00"))
                .transactionDate(baseDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .build();

        Transaction tx2 = Transaction.builder()
                .id(2L)
                .merchantName("Netflix India")
                .description("Netflix.com payment")
                .amount(new BigDecimal("649.00"))
                .transactionDate(baseDate.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .build();

        Transaction tx3 = Transaction.builder()
                .id(3L)
                .merchantName("Netflix India")
                .description("Netflix Mumbai autopay")
                .amount(new BigDecimal("649.00"))
                .transactionDate(baseDate.plusMonths(2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .build();

        when(transactionRepository.findExpensesByUserIdSince(eq(userId), any(Instant.class)))
                .thenReturn(List.of(tx1, tx2, tx3));

        when(recurringRepository.findByUserIdOrderByNextExecutionDateAsc(userId))
                .thenReturn(Collections.emptyList());

        SubscriptionDetectionSummaryResponse summary = subscriptionDetectorService.detectSubscriptions(userId, 6);

        assertThat(summary.getDetectedSubscriptionsCount()).isEqualTo(1);
        DetectedSubscriptionResponse detected = summary.getSubscriptions().get(0);

        // Core requirement verifications
        assertThat(detected.getSubscriptionName()).containsIgnoringCase("Netflix");
        assertThat(detected.getAveragePayment()).isEqualByComparingTo(new BigDecimal("649.00"));
        assertThat(detected.getFrequency()).isEqualTo(RecurringFrequency.MONTHLY);
        assertThat(detected.getEstimatedAnnualCost()).isEqualByComparingTo(new BigDecimal("7788.00")); // 649 * 12
        assertThat(detected.getLastPaymentDate()).isEqualTo(baseDate.plusMonths(2));

        // Must be clearly labeled as suggestion
        assertThat(detected.getStatus()).isEqualTo("SUGGESTION");
        assertThat(detected.isSuggestion()).isTrue();
        assertThat(detected.getConfidenceScore()).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    @DisplayName("Should detect weekly recurring subscriptions and compute annual cost")
    void testDetectWeeklySubscription() {
        LocalDate baseDate = LocalDate.now().minusWeeks(4);

        Transaction tx1 = Transaction.builder()
                .id(11L)
                .merchantName("Milk Basket")
                .description("Weekly Grocery Delivery")
                .amount(new BigDecimal("500.00"))
                .transactionDate(baseDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .build();

        Transaction tx2 = Transaction.builder()
                .id(12L)
                .merchantName("Milk Basket")
                .description("Weekly Grocery Delivery")
                .amount(new BigDecimal("500.00"))
                .transactionDate(baseDate.plusWeeks(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .build();

        Transaction tx3 = Transaction.builder()
                .id(13L)
                .merchantName("Milk Basket")
                .description("Weekly Grocery Delivery")
                .amount(new BigDecimal("500.00"))
                .transactionDate(baseDate.plusWeeks(2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .build();

        when(transactionRepository.findExpensesByUserIdSince(eq(userId), any(Instant.class)))
                .thenReturn(List.of(tx1, tx2, tx3));

        when(recurringRepository.findByUserIdOrderByNextExecutionDateAsc(userId))
                .thenReturn(Collections.emptyList());

        SubscriptionDetectionSummaryResponse summary = subscriptionDetectorService.detectSubscriptions(userId, 6);

        assertThat(summary.getDetectedSubscriptionsCount()).isEqualTo(1);
        DetectedSubscriptionResponse detected = summary.getSubscriptions().get(0);

        assertThat(detected.getFrequency()).isEqualTo(RecurringFrequency.WEEKLY);
        assertThat(detected.getAveragePayment()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(detected.getEstimatedAnnualCost()).isEqualByComparingTo(new BigDecimal("26000.00")); // 500 * 52
        assertThat(detected.isSuggestion()).isTrue();
    }

    @Test
    @DisplayName("Should ignore random one-off purchases that have no recurring date pattern")
    void testIgnoreRandomPurchases() {
        LocalDate now = LocalDate.now();

        Transaction random1 = Transaction.builder()
                .id(21L)
                .merchantName("Amazon")
                .amount(new BigDecimal("1200.00"))
                .transactionDate(now.minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC))
                .transactionType(TransactionType.EXPENSE)
                .build();

        Transaction random2 = Transaction.builder()
                .id(22L)
                .merchantName("Amazon")
                .amount(new BigDecimal("450.00")) // Different amount, random 2-day gap
                .transactionDate(now.minusDays(8).atStartOfDay().toInstant(ZoneOffset.UTC))
                .transactionType(TransactionType.EXPENSE)
                .build();

        when(transactionRepository.findExpensesByUserIdSince(eq(userId), any(Instant.class)))
                .thenReturn(List.of(random1, random2));

        when(recurringRepository.findByUserIdOrderByNextExecutionDateAsc(userId))
                .thenReturn(Collections.emptyList());

        SubscriptionDetectionSummaryResponse summary = subscriptionDetectorService.detectSubscriptions(userId, 6);

        assertThat(summary.getDetectedSubscriptionsCount()).isEqualTo(0);
    }
}
