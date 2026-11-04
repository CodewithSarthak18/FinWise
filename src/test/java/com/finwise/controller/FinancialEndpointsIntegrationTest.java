package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.dto.budget.BudgetCategoryRequest;
import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.goal.GoalContributionRequest;
import com.finwise.dto.goal.GoalRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.transaction.TransferRequest;
import com.finwise.entity.AccountType;
import com.finwise.entity.BudgetPeriodType;
import com.finwise.entity.CategoryType;
import com.finwise.entity.GoalPriority;
import com.finwise.entity.PaymentMode;
import com.finwise.entity.TransactionType;
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
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinancialEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueEmail = "user." + System.currentTimeMillis() + "@finwise.in";
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Priya")
                .lastName("Nair")
                .email(uniqueEmail)
                .password("StrongPassword#2026")
                .currencyCode("INR")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = registerResult.getResponse().getContentAsString();
        authToken = objectMapper.readTree(responseJson).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Complete Financial Lifecycle: Accounts -> Categories -> Transactions -> Transfers -> Budgets -> Goals -> Analytics")
    void verifyCompleteFinancialLifecycle() throws Exception {
        // 1. Create Savings Account with ₹50,000.00
        AccountRequest acc1Req = AccountRequest.builder()
                .accountName("HDFC Primary Savings")
                .accountType(AccountType.SAVINGS)
                .institutionName("HDFC Bank")
                .initialBalance(new BigDecimal("50000.00"))
                .currencyCode("INR")
                .build();

        MvcResult acc1Result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acc1Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currentBalance", is("50000.00")))
                .andReturn();

        Long acc1Id = objectMapper.readTree(acc1Result.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Create Second Account (Trading/Investment) with ₹10,000.00
        AccountRequest acc2Req = AccountRequest.builder()
                .accountName("Zerodha Demat")
                .accountType(AccountType.INVESTMENT)
                .institutionName("Zerodha")
                .initialBalance(new BigDecimal("10000.00"))
                .currencyCode("INR")
                .build();

        MvcResult acc2Result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acc2Req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long acc2Id = objectMapper.readTree(acc2Result.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Create Custom Expense Category
        CategoryRequest catReq = CategoryRequest.builder()
                .name("Dining & Swiggy")
                .type(CategoryType.EXPENSE)
                .icon("utensils")
                .colorCode("#F97316")
                .build();

        MvcResult catResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long catId = objectMapper.readTree(catResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 4. Record Expense of ₹2,500.50 from Acc1
        TransactionRequest expenseReq = TransactionRequest.builder()
                .accountId(acc1Id)
                .categoryId(catId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("2500.50"))
                .transactionDate(Instant.now())
                .merchantName("Swiggy Orders")
                .paymentMode(PaymentMode.UPI)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount", is("2500.50")));

        // Verify Account balance was deducted: 50,000.00 - 2,500.50 = 47,499.50
        mockMvc.perform(get("/api/v1/accounts/" + acc1Id)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is("47499.50")));

        // 5. Test Insufficient Funds Exception: Attempt to spend ₹60,000.00 when balance is ₹47,499.50
        TransactionRequest overdrawnReq = TransactionRequest.builder()
                .accountId(acc1Id)
                .categoryId(catId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("60000.00"))
                .transactionDate(Instant.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overdrawnReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INSUFFICIENT_FUNDS")));

        // 6. Execute Atomic Transfer of ₹15,000.00 from Acc1 to Acc2
        TransferRequest transferReq = TransferRequest.builder()
                .fromAccountId(acc1Id)
                .toAccountId(acc2Id)
                .amount(new BigDecimal("15000.00"))
                .transferDate(Instant.now())
                .note("Monthly Equity Investment Transfer")
                .build();

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount", is("15000.00")));

        // Verify both account balances after transfer:
        // Acc1: 47,499.50 - 15,000.00 = 32,499.50
        // Acc2: 10,000.00 + 15,000.00 = 25,000.00
        mockMvc.perform(get("/api/v1/accounts/" + acc1Id)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(jsonPath("$.data.currentBalance", is("32499.50")));

        mockMvc.perform(get("/api/v1/accounts/" + acc2Id)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(jsonPath("$.data.currentBalance", is("25000.00")));

        // 7. Create Budget for current month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        BudgetCategoryRequest bcat = BudgetCategoryRequest.builder()
                .categoryId(catId)
                .allocatedAmount(new BigDecimal("10000.00"))
                .build();

        BudgetRequest budgetReq = BudgetRequest.builder()
                .name("Monthly Household Budget")
                .periodType(BudgetPeriodType.MONTHLY)
                .startDate(startOfMonth)
                .endDate(endOfMonth)
                .totalBudgetAmount(new BigDecimal("40000.00"))
                .notificationThreshold(new BigDecimal("80.00"))
                .categoryAllocations(Collections.singletonList(bcat))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalBudgetAmount", is("40000.00")));

        // 8. Create Savings Goal and contribute ₹5,000.00 from Acc1
        GoalRequest goalReq = GoalRequest.builder()
                .title("Emergency Fund Corpus")
                .targetAmount(new BigDecimal("200000.00"))
                .targetDate(LocalDate.now().plusMonths(12))
                .priority(GoalPriority.HIGH)
                .build();

        MvcResult goalResult = mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long goalId = objectMapper.readTree(goalResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        GoalContributionRequest contribReq = GoalContributionRequest.builder()
                .accountId(acc1Id)
                .amount(new BigDecimal("5000.00"))
                .note("Initial goal allocation")
                .build();

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/contribute")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contribReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount", is("5000.00")));

        // 9. Verify Dashboard Analytics Summary
        // Total Net Worth: Acc1 (27,499.50) + Acc2 (25,000.00) = 52,499.50
        mockMvc.perform(get("/api/v1/analytics/dashboard-summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.netWorth", is("52499.50")))
                .andExpect(jsonPath("$.data.monthlyExpense", is("2500.50")))
                .andExpect(jsonPath("$.data.activeAccountsCount", is(2)))
                .andExpect(jsonPath("$.data.hasActiveBudget", is(true)));
    }
}
