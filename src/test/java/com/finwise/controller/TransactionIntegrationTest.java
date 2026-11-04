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
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long accountId;
    private Long foodCategoryId;
    private Long salaryCategoryId;

    @BeforeEach
    void setup() throws Exception {
        String uniqueSuffix = String.valueOf(System.nanoTime());

        // 1. Register User
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Siddharth")
                .lastName("Mehta")
                .email("sid." + uniqueSuffix + "@finwise.in")
                .password("Password#2026")
                .currencyCode("INR")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        token = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 2. Create Bank Account with ₹50,000.00
        AccountRequest accReq = AccountRequest.builder()
                .accountName("ICICI Bank Savings")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("50000.00"))
                .institutionName("ICICI Bank")
                .build();

        MvcResult accResult = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReq)))
                .andExpect(status().isCreated())
                .andReturn();

        accountId = objectMapper.readTree(accResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Create Food Expense Category
        CategoryRequest foodCat = CategoryRequest.builder()
                .name("Food & Dining " + uniqueSuffix)
                .type(CategoryType.EXPENSE)
                .colorCode("#FF5722")
                .icon("utensils")
                .build();

        MvcResult foodResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foodCat)))
                .andExpect(status().isCreated())
                .andReturn();

        foodCategoryId = objectMapper.readTree(foodResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 4. Create Salary Income Category
        CategoryRequest salaryCat = CategoryRequest.builder()
                .name("Salary Income " + uniqueSuffix)
                .type(CategoryType.INCOME)
                .colorCode("#4CAF50")
                .icon("briefcase")
                .build();

        MvcResult salaryResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salaryCat)))
                .andExpect(status().isCreated())
                .andReturn();

        salaryCategoryId = objectMapper.readTree(salaryResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("Complete Transaction Lifecycle: Add Expense -> Add Income -> Edit -> Delete -> Balance Recalculation")
    void testCompleteTransactionLifecycleAndBalanceUpdates() throws Exception {
        // 1. Add Expense (₹4,500.50) with Notes, Payment Method, Description
        TransactionRequest expenseReq = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(foodCategoryId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("4500.50"))
                .transactionDate(Instant.now())
                .merchantName("Blinkit Quick Commerce")
                .description("Weekly grocery and essentials")
                .notes("Bought organic vegetables and dairy")
                .paymentMode(PaymentMode.UPI)
                .referenceNumber("UPI-TXN-987654")
                .build();

        MvcResult expenseResult = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.amount", is("4500.50")))
                .andExpect(jsonPath("$.data.notes", is("Bought organic vegetables and dairy")))
                .andExpect(jsonPath("$.data.paymentMode", is("UPI")))
                .andReturn();

        Long expenseId = objectMapper.readTree(expenseResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Verify Account Balance deducted from ₹50,000.00 to ₹45,499.50
        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is("45499.50")));

        // 2. Add Income (₹20,000.00)
        TransactionRequest incomeReq = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(salaryCategoryId)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("20000.00"))
                .transactionDate(Instant.now())
                .merchantName("Consulting Client")
                .description("Milestone 1 consulting fee")
                .paymentMode(PaymentMode.NET_BANKING)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount", is("20000.00")));

        // Verify Account Balance increased to ₹65,499.50
        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is("65499.50")));

        // 3. Edit Expense: Change amount from ₹4,500.50 to ₹6,000.00 (additional ₹1,499.50 deducted)
        TransactionRequest updateReq = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(foodCategoryId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("6000.00"))
                .transactionDate(Instant.now())
                .merchantName("Blinkit & Nature's Basket")
                .description("Updated grocery expense")
                .notes("Added imported cheese and olive oil")
                .paymentMode(PaymentMode.CREDIT_CARD)
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + expenseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount", is("6000.00")))
                .andExpect(jsonPath("$.data.notes", is("Added imported cheese and olive oil")));

        // Verify Account Balance updated: 65499.50 - 1499.50 = 64000.00
        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is("64000.00")));

        // 4. Delete Transaction: Deleting the ₹6,000.00 expense restores balance to ₹70,000.00
        mockMvc.perform(delete("/api/v1/transactions/" + expenseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Transaction deleted successfully")));

        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is("70000.00")));

        // Confirm deleted transaction returns 404
        mockMvc.perform(get("/api/v1/transactions/" + expenseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Search, Category, Account, Date Range Filtering and Sorting")
    void testSearchFilterAndSorting() throws Exception {
        Instant now = Instant.now();

        // Transaction 1: Food expense today
        TransactionRequest tx1 = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(foodCategoryId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("1200.00"))
                .transactionDate(now)
                .merchantName("Swiggy Biryani")
                .description("Family dinner on Sunday")
                .notes("Special weekend treat")
                .paymentMode(PaymentMode.UPI)
                .build();

        // Transaction 2: Food expense 5 days ago
        TransactionRequest tx2 = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(foodCategoryId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("350.00"))
                .transactionDate(now.minus(5, ChronoUnit.DAYS))
                .merchantName("Starbucks Coffee")
                .description("Cappuccino and croissant")
                .paymentMode(PaymentMode.DEBIT_CARD)
                .build();

        // Transaction 3: Income 10 days ago
        TransactionRequest tx3 = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(salaryCategoryId)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("80000.00"))
                .transactionDate(now.minus(10, ChronoUnit.DAYS))
                .merchantName("Acme Corp")
                .description("Monthly base salary credit")
                .paymentMode(PaymentMode.NET_BANKING)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx1))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx2))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx3))).andExpect(status().isCreated());

        // 1. Search transactions by keyword in description/notes ("Biryani")
        mockMvc.perform(get("/api/v1/transactions?search=Biryani")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].merchantName", is("Swiggy Biryani")));

        // 2. Filter by category (Food Category only) -> should return 2 transactions
        mockMvc.perform(get("/api/v1/transactions?categoryId=" + foodCategoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // 3. Filter by account
        mockMvc.perform(get("/api/v1/transactions?accountId=" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)));

        // 4. Filter by date range: last 7 days only -> should return 2 transactions (excludes 10 days ago)
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        mockMvc.perform(get("/api/v1/transactions?startDate=" + sevenDaysAgo + "&endDate=" + now.plus(1, ChronoUnit.DAYS))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // 5. Sort by amount ASC ("350.00" -> "1200.00" -> "80000.00")
        mockMvc.perform(get("/api/v1/transactions?sort=amount,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].amount", is("350.00")))
                .andExpect(jsonPath("$.data.content[2].amount", is("80000.00")));
    }

    @Test
    @DisplayName("Prevent Duplicate Processing: Submitting identical reference number returns 409 Conflict")
    void testPreventDuplicateProcessing() throws Exception {
        String referenceNumber = "UTR-HDFC-9911223344";

        TransactionRequest txRequest = TransactionRequest.builder()
                .accountId(accountId)
                .categoryId(foodCategoryId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("750.00"))
                .transactionDate(Instant.now())
                .merchantName("Dominos Pizza")
                .description("Dinner order")
                .referenceNumber(referenceNumber)
                .build();

        // First submission succeeds
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txRequest)))
                .andExpect(status().isCreated());

        // Duplicate submission with same reference number returns 409 Conflict
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString(referenceNumber)));
    }
}
