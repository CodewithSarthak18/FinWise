package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.subscription.ConfirmSubscriptionRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.whatif.WhatIfSimulationRequest;
import com.finwise.entity.AccountType;
import com.finwise.entity.CategoryType;
import com.finwise.entity.PaymentMode;
import com.finwise.entity.RecurringFrequency;
import com.finwise.entity.TransactionType;
import com.finwise.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Subscription Detector & What-If Calculator Integration Tests")
class SubscriptionAndWhatIfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    private String authToken;
    private Long accountId;
    private Long ottCatId;
    private Long diningCatId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueEmail = "subuser." + System.currentTimeMillis() + "@finwise.in";
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Siddharth")
                .lastName("Mehta")
                .email(uniqueEmail)
                .password("AdvanceSecure#2026")
                .currencyCode("INR")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        authToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Create Account with ₹75,000.00
        AccountRequest accReq = AccountRequest.builder()
                .accountName("Kotak Checking Account")
                .accountType(AccountType.SAVINGS)
                .institutionName("Kotak Mahindra Bank")
                .initialBalance(new BigDecimal("75000.00"))
                .currencyCode("INR")
                .build();

        MvcResult accResult = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReq)))
                .andExpect(status().isCreated())
                .andReturn();

        accountId = objectMapper.readTree(accResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Create Category 1: Subscriptions
        CategoryRequest ottCat = CategoryRequest.builder()
                .name("OTT Subscriptions " + System.currentTimeMillis())
                .type(CategoryType.EXPENSE)
                .icon("film")
                .colorCode("#6366F1")
                .build();
        MvcResult ottResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ottCat)))
                .andExpect(status().isCreated())
                .andReturn();
        ottCatId = objectMapper.readTree(ottResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Create Category 2: Food & Dining
        CategoryRequest diningCat = CategoryRequest.builder()
                .name("Food & Dining " + System.currentTimeMillis())
                .type(CategoryType.EXPENSE)
                .icon("utensils")
                .colorCode("#F97316")
                .build();
        MvcResult diningResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diningCat)))
                .andExpect(status().isCreated())
                .andReturn();
        diningCatId = objectMapper.readTree(diningResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Seed 3 monthly Spotify payments (to detect Spotify subscription)
        LocalDate today = LocalDate.now();
        for (int i = 2; i >= 0; i--) {
            LocalDate paymentDate = today.minusMonths(i);
            TransactionRequest tx = TransactionRequest.builder()
                    .accountId(accountId)
                    .categoryId(ottCatId)
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("119.00"))
                    .transactionDate(paymentDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                    .merchantName("Spotify India")
                    .description("Spotify Premium Monthly Subscription")
                    .paymentMode(PaymentMode.UPI)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tx)))
                    .andExpect(status().isCreated());
        }

        // Seed Dining expenses for What-If simulation (₹12,000.00 total)
        TransactionRequest dining1 = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(diningCatId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("7000.00"))
                .transactionDate(Instant.now())
                .merchantName("Bistro Fine Dine")
                .description("Family Dinner")
                .paymentMode(PaymentMode.CREDIT_CARD)
                .build();
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dining1)))
                .andExpect(status().isCreated());

        TransactionRequest dining2 = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(diningCatId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("5000.00"))
                .transactionDate(Instant.now())
                .merchantName("Starbucks")
                .description("Weekly Coffee & Snacks")
                .paymentMode(PaymentMode.UPI)
                .build();
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dining2)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Subscription Detector: Identify recurring expenses, verify labels and annual projections")
    void testSubscriptionDetectionLifecycle() throws Exception {
        // 1. Detect subscriptions
        MvcResult detectedResult = mockMvc.perform(get("/api/v1/subscriptions/detected?months=6")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detectedSubscriptionsCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.subscriptions[0].subscriptionName", containsString("Spotify")))
                .andExpect(jsonPath("$.data.subscriptions[0].averagePayment", is("119.00")))
                .andExpect(jsonPath("$.data.subscriptions[0].frequency", is("MONTHLY")))
                .andExpect(jsonPath("$.data.subscriptions[0].estimatedAnnualCost", is("1428.00"))) // 119 * 12
                .andExpect(jsonPath("$.data.subscriptions[0].status", is("SUGGESTION")))
                .andExpect(jsonPath("$.data.subscriptions[0].suggestion", is(true)))
                .andExpect(jsonPath("$.data.subscriptions[0].occurrenceCount", is(3)))
                .andReturn();

        String candidateId = objectMapper.readTree(detectedResult.getResponse().getContentAsString())
                .path("data").path("subscriptions").get(0).path("candidateId").asText();

        // 2. User confirms detected subscription -> creates recurring transaction
        ConfirmSubscriptionRequest confirmReq = ConfirmSubscriptionRequest.builder()
                .subscriptionName("Spotify Premium")
                .accountId(accountId)
                .categoryId(ottCatId)
                .amount(new BigDecimal("119.00"))
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(LocalDate.now())
                .autoPost(true)
                .build();

        mockMvc.perform(post("/api/v1/subscriptions/confirm")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("Spotify Premium")))
                .andExpect(jsonPath("$.data.amount", is("119.00")))
                .andExpect(jsonPath("$.data.frequency", is("MONTHLY")));

        // 3. User can dismiss a suggestion
        mockMvc.perform(post("/api/v1/subscriptions/" + candidateId + "/dismiss")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("dismissed")));
    }

    @Test
    @DisplayName("What-If Calculator: Simulate 10% and 20% spending reduction and verify no DB records modified")
    void testWhatIfSavingsSimulation() throws Exception {
        long txCountBefore = transactionRepository.count();

        // Simulate 10% reduction on Dining Out (₹12,000.00 spending)
        WhatIfSimulationRequest request10 = WhatIfSimulationRequest.builder()
                .categoryId(diningCatId)
                .reductionPercentage(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/v1/calculator/what-if")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentMonthlySpending", is("12000.00")))
                .andExpect(jsonPath("$.data.reductionPercentage", is("10.00")))
                .andExpect(jsonPath("$.data.monthlySavings", is("1200.00")))        // 12,000 * 10%
                .andExpect(jsonPath("$.data.reducedMonthlySpending", is("10800.00"))) // 12,000 - 1,200
                .andExpect(jsonPath("$.data.estimatedYearlySavings", is("14400.00"))) // 1,200 * 12
                .andExpect(jsonPath("$.data.scenarios", hasSize(5)))
                .andExpect(jsonPath("$.data.notice", containsString("NOT been modified")));

        // Simulate 20% reduction on Dining Out (₹12,000.00 spending)
        WhatIfSimulationRequest request20 = WhatIfSimulationRequest.builder()
                .categoryId(diningCatId)
                .reductionPercentage(new BigDecimal("20.00"))
                .build();

        mockMvc.perform(post("/api/v1/calculator/what-if")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentMonthlySpending", is("12000.00")))
                .andExpect(jsonPath("$.data.reductionPercentage", is("20.00")))
                .andExpect(jsonPath("$.data.monthlySavings", is("2400.00")))        // 12,000 * 20%
                .andExpect(jsonPath("$.data.reducedMonthlySpending", is("9600.00")))  // 12,000 - 2,400
                .andExpect(jsonPath("$.data.estimatedYearlySavings", is("28800.00"))); // 2,400 * 12

        // CRITICAL INVARIANT: Real transaction records must NOT be modified
        long txCountAfter = transactionRepository.count();
        assertThat(txCountAfter).isEqualTo(txCountBefore);
    }

    @Test
    @DisplayName("What-If Calculator: Validation should reject invalid percentages (<= 0 or > 100)")
    void testWhatIfValidation() throws Exception {
        // Test invalid negative or zero percentage
        WhatIfSimulationRequest zeroReq = WhatIfSimulationRequest.builder()
                .categoryId(diningCatId)
                .reductionPercentage(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/calculator/what-if")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.reductionPercentage", notNullValue()));

        // Test invalid percentage > 100%
        WhatIfSimulationRequest excessReq = WhatIfSimulationRequest.builder()
                .categoryId(diningCatId)
                .reductionPercentage(new BigDecimal("105.00"))
                .build();

        mockMvc.perform(post("/api/v1/calculator/what-if")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.reductionPercentage", notNullValue()));
    }

    @Test
    @DisplayName("What-If Calculator: Query category spending options for interactive UI selection")
    void testWhatIfCategories() throws Exception {
        mockMvc.perform(get("/api/v1/calculator/what-if/categories")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }
}
