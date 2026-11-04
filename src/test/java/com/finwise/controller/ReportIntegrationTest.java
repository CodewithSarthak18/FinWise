package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.dto.budget.BudgetCategoryRequest;
import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.goal.GoalRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.entity.AccountType;
import com.finwise.entity.BudgetPeriodType;
import com.finwise.entity.CategoryType;
import com.finwise.entity.GoalPriority;
import com.finwise.entity.PaymentMode;
import com.finwise.entity.TransactionType;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.UserRepository;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String token;
    private Long accountId;
    private Long salaryCatId;
    private Long foodCatId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueSuffix = String.valueOf(System.nanoTime());

        // 1. Register User
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Ananya")
                .lastName("Iyer")
                .email("ananya." + uniqueSuffix + "@finwise.test")
                .password("SecureFinPass#2026")
                .currencyCode("INR")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        token = "Bearer " + objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 2. Create Account
        AccountRequest accReq = AccountRequest.builder()
                .accountName("HDFC Salary Account")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("100000.00"))
                .institutionName("HDFC Bank")
                .build();

        MvcResult accRes = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReq)))
                .andExpect(status().isCreated())
                .andReturn();

        accountId = objectMapper.readTree(accRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Create Salary & Food Categories
        CategoryRequest salaryReq = CategoryRequest.builder()
                .name("Salary Income " + uniqueSuffix)
                .type(CategoryType.INCOME)
                .colorCode("#059669")
                .icon("briefcase")
                .build();

        MvcResult salRes = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryReq)))
                .andExpect(status().isCreated())
                .andReturn();

        salaryCatId = objectMapper.readTree(salRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        CategoryRequest foodReq = CategoryRequest.builder()
                .name("Food & Dining " + uniqueSuffix)
                .type(CategoryType.EXPENSE)
                .colorCode("#DC2626")
                .icon("utensils")
                .build();

        MvcResult foodRes = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foodReq)))
                .andExpect(status().isCreated())
                .andReturn();

        foodCatId = objectMapper.readTree(foodRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 4. Create Income Transaction (₹1,25,000.00)
        TransactionRequest incReq = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(salaryCatId)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("125000.00"))
                .transactionDate(Instant.now())
                .description("Monthly Tech Salary")
                .paymentMode(PaymentMode.NET_BANKING)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incReq)))
                .andExpect(status().isCreated());

        // 5. Create Expense Transaction (₹15,000.00)
        TransactionRequest expReq = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(foodCatId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("15000.00"))
                .transactionDate(Instant.now())
                .description("Organic Groceries")
                .paymentMode(PaymentMode.UPI)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated());

        // 6. Create Active Budget
        BudgetRequest budgetReq = BudgetRequest.builder()
                .name("Monthly Household Budget")
                .periodType(BudgetPeriodType.MONTHLY)
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1))
                .totalBudgetAmount(new BigDecimal("50000.00"))
                .notificationThreshold(new BigDecimal("80.00"))
                .categoryAllocations(List.of(
                        BudgetCategoryRequest.builder()
                                .categoryId(foodCatId)
                                .allocatedAmount(new BigDecimal("20000.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetReq)))
                .andExpect(status().isCreated());

        // 7. Create Savings Goal
        GoalRequest goalReq = GoalRequest.builder()
                .title("Emergency Reserve Fund")
                .targetAmount(new BigDecimal("500000.00"))
                .targetDate(LocalDate.now().plusYears(1))
                .priority(GoalPriority.HIGH)
                .build();

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Unauthenticated request to reports should return 401 Unauthorized")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/reports/monthly"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("1. Export Monthly Financial Report as PDF & CSV & JSON")
    void testMonthlyReportExport() throws Exception {
        // PDF Export
        MvcResult pdfResult = mockMvc.perform(get("/api/v1/reports/monthly")
                        .header("Authorization", token)
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertThat(new String(pdfBytes, 0, 8, StandardCharsets.US_ASCII)).startsWith("%PDF-1.4");

        // CSV Export
        MvcResult csvResult = mockMvc.perform(get("/api/v1/reports/monthly")
                        .header("Authorization", token)
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andReturn();

        String csvContent = csvResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csvContent).contains("Total Income,\"Rs. 1,25,000.00\"");
        assertThat(csvContent).contains("Total Expenses,\"Rs. 15,000.00\"");
        assertThat(csvContent).contains("Net Savings,\"Rs. 1,10,000.00\"");

        // JSON Data Preview
        mockMvc.perform(get("/api/v1/reports/monthly")
                        .header("Authorization", token)
                        .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.formattedTotalIncome", is("Rs. 1,25,000.00")))
                .andExpect(jsonPath("$.data.formattedTotalExpenses", is("Rs. 15,000.00")));
    }

    @Test
    @DisplayName("2. Export Annual Financial Report as PDF & CSV")
    void testAnnualReportExport() throws Exception {
        int currentYear = LocalDate.now().getYear();

        // PDF
        MvcResult pdfRes = mockMvc.perform(get("/api/v1/reports/annual")
                        .header("Authorization", token)
                        .param("year", String.valueOf(currentYear))
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        assertThat(pdfRes.getResponse().getContentAsByteArray()).isNotEmpty();

        // CSV
        MvcResult csvRes = mockMvc.perform(get("/api/v1/reports/annual")
                        .header("Authorization", token)
                        .param("year", String.valueOf(currentYear))
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = csvRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Annual Financial Report - Year " + currentYear);
        assertThat(csv).contains("--- MONTH-BY-MONTH BREAKDOWN ---");
    }

    @Test
    @DisplayName("3. Export Category-wise Report as PDF & CSV")
    void testCategoryReportExport() throws Exception {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();

        MvcResult pdfRes = mockMvc.perform(get("/api/v1/reports/category-wise")
                        .header("Authorization", token)
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        assertThat(pdfRes.getResponse().getContentAsByteArray()).isNotEmpty();

        MvcResult csvRes = mockMvc.perform(get("/api/v1/reports/category-wise")
                        .header("Authorization", token)
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = csvRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Category-Wise Financial Report");
    }

    @Test
    @DisplayName("4. Export Income and Expense Summary as PDF & CSV")
    void testIncomeExpenseReportExport() throws Exception {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();

        MvcResult pdfRes = mockMvc.perform(get("/api/v1/reports/income-expense")
                        .header("Authorization", token)
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        assertThat(pdfRes.getResponse().getContentAsByteArray()).isNotEmpty();

        MvcResult csvRes = mockMvc.perform(get("/api/v1/reports/income-expense")
                        .header("Authorization", token)
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = csvRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("50/30/20 BUDGET ANALYSIS");
        assertThat(csv).contains("Needs");
    }

    @Test
    @DisplayName("5. Export Savings Summary Report as PDF & CSV")
    void testSavingsReportExport() throws Exception {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();

        MvcResult pdfRes = mockMvc.perform(get("/api/v1/reports/savings")
                        .header("Authorization", token)
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        assertThat(pdfRes.getResponse().getContentAsByteArray()).isNotEmpty();

        MvcResult csvRes = mockMvc.perform(get("/api/v1/reports/savings")
                        .header("Authorization", token)
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = csvRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Savings & Wealth Goals Report");
        assertThat(csv).contains("Emergency Reserve Fund");
    }

    @Test
    @DisplayName("6. Export Budget Performance Report as PDF & CSV")
    void testBudgetPerformanceReportExport() throws Exception {
        MvcResult pdfRes = mockMvc.perform(get("/api/v1/reports/budget-performance")
                        .header("Authorization", token)
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        assertThat(pdfRes.getResponse().getContentAsByteArray()).isNotEmpty();

        MvcResult csvRes = mockMvc.perform(get("/api/v1/reports/budget-performance")
                        .header("Authorization", token)
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = csvRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Budget Performance & Variance Report");
        assertThat(csv).contains("Monthly Household Budget");
    }

    @Test
    @DisplayName("Date Range Validation: startDate after endDate returns 400 Bad Request")
    void testInvalidDateRangeValidation() throws Exception {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(5); // Invalid: end before start

        mockMvc.perform(get("/api/v1/reports/income-expense")
                        .header("Authorization", token)
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("format", "pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("startDate cannot be after endDate")));
    }
}
