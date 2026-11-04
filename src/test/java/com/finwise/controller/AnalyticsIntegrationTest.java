package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.entity.AccountType;
import com.finwise.entity.CategoryType;
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
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Financial Analytics REST APIs Integration Tests")
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private Long accountId;
    private Long rentCatId;
    private Long foodCatId;
    private Long salaryCatId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueEmail = "analyst." + System.currentTimeMillis() + "@finwise.in";
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Rohan")
                .lastName("Verma")
                .email(uniqueEmail)
                .password("AnalysisPass#2026")
                .currencyCode("INR")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        authToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Account with ₹100,000.00
        AccountRequest accReq = AccountRequest.builder()
                .accountName("Axis Salary Account")
                .accountType(AccountType.SAVINGS)
                .institutionName("Axis Bank")
                .initialBalance(new BigDecimal("100000.00"))
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

        // 1. Rent Category (Needs)
        CategoryRequest rentCat = CategoryRequest.builder()
                .name("Apartment Rent " + System.currentTimeMillis())
                .type(CategoryType.EXPENSE)
                .icon("home")
                .colorCode("#EF4444")
                .build();
        MvcResult rentResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rentCat)))
                .andExpect(status().isCreated())
                .andReturn();
        rentCatId = objectMapper.readTree(rentResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Food & Dining Category (Wants)
        CategoryRequest foodCat = CategoryRequest.builder()
                .name("Dining & Cafes " + System.currentTimeMillis())
                .type(CategoryType.EXPENSE)
                .icon("utensils")
                .colorCode("#F97316")
                .build();
        MvcResult foodResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foodCat)))
                .andExpect(status().isCreated())
                .andReturn();
        foodCatId = objectMapper.readTree(foodResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Salary Category (Income)
        CategoryRequest salaryCat = CategoryRequest.builder()
                .name("Monthly Salary " + System.currentTimeMillis())
                .type(CategoryType.INCOME)
                .icon("briefcase")
                .colorCode("#10B981")
                .build();
        MvcResult salaryResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryCat)))
                .andExpect(status().isCreated())
                .andReturn();
        salaryCatId = objectMapper.readTree(salaryResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Populate Actual Transactions for Current Month
        Instant now = Instant.now();

        // Income: ₹80,000.00
        TransactionRequest incomeTx = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(salaryCatId)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("80000.00"))
                .transactionDate(now)
                .paymentMode(PaymentMode.NET_BANKING)
                .merchantName("Tech Corp India")
                .description("Monthly Salary Payout")
                .build();
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeTx)))
                .andExpect(status().isCreated());

        // Expense 1: ₹25,000.00 (Rent - Need)
        TransactionRequest rentTx = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(rentCatId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("25000.00"))
                .transactionDate(now)
                .paymentMode(PaymentMode.NET_BANKING)
                .merchantName("Landlord")
                .description("Flat Rent November")
                .build();
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rentTx)))
                .andExpect(status().isCreated());

        // Expense 2: ₹5,000.00 (Dining - Want)
        TransactionRequest diningTx = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(foodCatId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("5000.00"))
                .transactionDate(now)
                .paymentMode(PaymentMode.UPI)
                .merchantName("Cafe Mocha")
                .description("Weekend Dining")
                .build();
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diningTx)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Verify Analytics Core APIs: Total Income, Expenses, Net Savings and Savings Rate")
    void testCoreAnalyticsEndpoints() throws Exception {
        String currentMonth = YearMonth.now(ZoneOffset.UTC).toString();

        // 1. Total Income (₹80,000.00)
        mockMvc.perform(get("/api/v1/analytics/total-income?month=" + currentMonth)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome", is("80000.00")));

        // 2. Total Expenses (₹30,000.00 = 25,000 + 5,000)
        mockMvc.perform(get("/api/v1/analytics/total-expenses?month=" + currentMonth)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalExpenses", is("30000.00")));

        // 3. Net Savings (₹50,000.00 = 80,000 - 30,000)
        mockMvc.perform(get("/api/v1/analytics/net-savings?month=" + currentMonth)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.netSavings", is("50000.00")));

        // 4. Savings Rate (62.5% = 50,000 / 80,000 * 100)
        mockMvc.perform(get("/api/v1/analytics/savings-rate?month=" + currentMonth)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savingsRate", is(62.5)));
    }

    @Test
    @DisplayName("Verify Category-wise Expenses Breakdown")
    void testCategoryExpensesBreakdown() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/category-expenses")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].amount", is("25000.00"))) // Top expense: Rent
                .andExpect(jsonPath("$.data[0].percentage", is(83.33)))
                .andExpect(jsonPath("$.data[1].amount", is("5000.00")))  // Second expense: Dining
                .andExpect(jsonPath("$.data[1].percentage", is(16.67)));
    }

    @Test
    @DisplayName("Verify Needs vs Wants Breakdown (50/30/20 Rule)")
    void testNeedsVsWantsBreakdown() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/needs-vs-wants")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalExpenses", is("30000.00")))
                .andExpect(jsonPath("$.data.needsAmount", is("25000.00"))) // Rent
                .andExpect(jsonPath("$.data.needsPercentage", is(83.33)))
                .andExpect(jsonPath("$.data.wantsAmount", is("5000.00")))  // Dining
                .andExpect(jsonPath("$.data.wantsPercentage", is(16.67)))
                .andExpect(jsonPath("$.data.benchmarkRule", containsString("50/30/20")))
                .andExpect(jsonPath("$.data.topNeedsCategories", hasSize(1)))
                .andExpect(jsonPath("$.data.topWantsCategories", hasSize(1)));
    }

    @Test
    @DisplayName("Verify Income vs Expense Comparison and Spending Trends")
    void testIncomeVsExpenseAndTrends() throws Exception {
        // Income vs Expense Comparison
        mockMvc.perform(get("/api/v1/analytics/income-vs-expense?months=3")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[2].status", is("SURPLUS")));

        // Monthly Spending Trends
        mockMvc.perform(get("/api/v1/analytics/spending-trends?months=3")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[2].totalSpending", is("30000.00")))
                .andExpect(jsonPath("$.data[2].highestExpenseAmount", is("25000.00")));
    }

    @Test
    @DisplayName("Verify Educational Financial Health Score and Disclaimer")
    void testFinancialHealthScore() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/financial-health-score")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore", greaterThan(50)))
                .andExpect(jsonPath("$.data.ratingBand", isIn(new String[]{"EXCELLENT", "GOOD", "FAIR"})))
                .andExpect(jsonPath("$.data.savingsRateComponent.maxScore", is(40.0)))
                .andExpect(jsonPath("$.data.budgetAdherenceComponent.maxScore", is(35.0)))
                .andExpect(jsonPath("$.data.spendingConsistencyComponent.maxScore", is(25.0)))
                .andExpect(jsonPath("$.data.disclaimer", containsString("NOT a credit score")))
                .andExpect(jsonPath("$.data.formulaExplanation", containsString("Savings Rate (40% weight)")));
    }

    @Test
    @DisplayName("Verify Smart Insights with Real Transaction Data")
    void testSmartInsights() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/smart-insights")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasEnoughData", is(true)))
                .andExpect(jsonPath("$.data.insights", not(empty())))
                .andExpect(jsonPath("$.data.insights[?(@.type == 'SURPLUS_ALERT')]", not(empty())));
    }

    @Test
    @DisplayName("Verify Comprehensive Overview Summary Endpoint")
    void testOverviewSummary() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome", is("80000.00")))
                .andExpect(jsonPath("$.data.totalExpenses", is("30000.00")))
                .andExpect(jsonPath("$.data.netSavings", is("50000.00")))
                .andExpect(jsonPath("$.data.savingsRate", is(62.5)))
                .andExpect(jsonPath("$.data.transactionCount", is(3)));
    }
}
