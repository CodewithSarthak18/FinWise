package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.goal.GoalContributionRequest;
import com.finwise.dto.goal.GoalRequest;
import com.finwise.dto.recurring.RecurringTransactionRequest;
import com.finwise.entity.AccountType;
import com.finwise.entity.CategoryType;
import com.finwise.entity.GoalPriority;
import com.finwise.entity.RecurringFrequency;
import com.finwise.entity.TransactionType;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.NotificationRepository;
import com.finwise.repository.RecurringTransactionRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.service.RecurringTransactionService;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Savings Goals and Recurring Payments Integration Tests")
class RecurringAndGoalsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecurringTransactionService recurringService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    private String authToken;
    private Long accountId;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueEmail = "saver." + System.currentTimeMillis() + "@finwise.in";
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Aditi")
                .lastName("Sharma")
                .email(uniqueEmail)
                .password("SecureFin@2026")
                .currencyCode("INR")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        authToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Create testing account with initial balance ₹50,000.00
        AccountRequest accReq = AccountRequest.builder()
                .accountName("Salary & Savings")
                .accountType(AccountType.SAVINGS)
                .institutionName("State Bank of India")
                .initialBalance(new BigDecimal("50000.00"))
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

        // Create category for recurring payments & goals
        CategoryRequest catReq = CategoryRequest.builder()
                .name("Housing & Bills " + System.currentTimeMillis())
                .type(CategoryType.EXPENSE)
                .icon("home")
                .colorCode("#EF4444")
                .build();

        MvcResult catResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andReturn();

        categoryId = objectMapper.readTree(catResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("Savings Goal Full Lifecycle: Create -> Record Contributions -> Progress History -> Completion")
    void testSavingsGoalLifecycle() throws Exception {
        // 1. Create Savings Goal for ₹20,000.00
        GoalRequest goalRequest = GoalRequest.builder()
                .title("Emergency Fund 2026")
                .description("Build emergency reserve")
                .targetAmount(new BigDecimal("20000.00"))
                .targetDate(LocalDate.now().plusMonths(6))
                .priority(GoalPriority.HIGH)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("Emergency Fund 2026")))
                .andExpect(jsonPath("$.data.targetAmount", is("20000.00")))
                .andExpect(jsonPath("$.data.currentAmount", is("0.00")))
                .andExpect(jsonPath("$.data.remainingAmount", is("20000.00")))
                .andExpect(jsonPath("$.data.progressPercentage", is(0.0)))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")))
                .andReturn();

        Long goalId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. First contribution of ₹8,000.00 (40.0% progress)
        GoalContributionRequest contrib1 = GoalContributionRequest.builder()
                .accountId(accountId)
                .amount(new BigDecimal("8000.00"))
                .note("Initial milestone savings")
                .build();

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/contribute")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contrib1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount", is("8000.00")))
                .andExpect(jsonPath("$.data.remainingAmount", is("12000.00")))
                .andExpect(jsonPath("$.data.progressPercentage", is(40.0)))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));

        // Verify account balance was debited: 50,000 - 8,000 = 42,000
        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is("42000.00")));

        // 3. Second contribution of ₹12,000.00 (achieves 100% completion)
        GoalContributionRequest contrib2 = GoalContributionRequest.builder()
                .accountId(accountId)
                .amount(new BigDecimal("12000.00"))
                .note("Final target achievement")
                .build();

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/contribute")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contrib2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount", is("20000.00")))
                .andExpect(jsonPath("$.data.remainingAmount", is("0.00")))
                .andExpect(jsonPath("$.data.progressPercentage", is(100.0)))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")));

        // 4. Verify progress history returns both contributions
        mockMvc.perform(get("/api/v1/goals/" + goalId + "/contributions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].amount", is("12000.00")))
                .andExpect(jsonPath("$.data[0].note", is("Final target achievement")))
                .andExpect(jsonPath("$.data[1].amount", is("8000.00")))
                .andExpect(jsonPath("$.data[1].note", is("Initial milestone savings")));

        // 5. Attempting to contribute to an already completed goal should fail
        GoalContributionRequest extraContrib = GoalContributionRequest.builder()
                .accountId(accountId)
                .amount(new BigDecimal("1000.00"))
                .note("Over-contribution")
                .build();

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/contribute")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraContrib)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already fully achieved")));
    }

    @Test
    @DisplayName("Recurring Payments Lifecycle: Create -> Pause -> Resume -> Upcoming Reminders")
    void testRecurringPaymentsLifecycle() throws Exception {
        LocalDate startDate = LocalDate.now();
        LocalDate nextExecution = startDate.plusDays(3);

        RecurringTransactionRequest request = RecurringTransactionRequest.builder()
                .title("Internet Broadband Fibernet")
                .accountId(accountId)
                .categoryId(categoryId)
                .amount(new BigDecimal("1299.00"))
                .transactionType(TransactionType.EXPENSE)
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(startDate)
                .nextExecutionDate(nextExecution)
                .autoPost(true)
                .description("Monthly 300 Mbps unlimited plan")
                .build();

        // 1. Create recurring transaction
        MvcResult createResult = mockMvc.perform(post("/api/v1/recurring")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("Internet Broadband Fibernet")))
                .andExpect(jsonPath("$.data.amount", is("1299.00")))
                .andExpect(jsonPath("$.data.frequency", is("MONTHLY")))
                .andExpect(jsonPath("$.data.active", is(true)))
                .andExpect(jsonPath("$.data.autoPost", is(true)))
                .andReturn();

        Long recurringId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Fetch upcoming reminders (due in 3 days)
        mockMvc.perform(get("/api/v1/recurring/upcoming?days=7")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.id == " + recurringId + ")].title", contains("Internet Broadband Fibernet")))
                .andExpect(jsonPath("$.data[?(@.id == " + recurringId + ")].daysUntilDue", contains(3)));

        // 3. Pause recurrence
        mockMvc.perform(patch("/api/v1/recurring/" + recurringId + "/pause")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)));

        // 4. Verify paused recurring payment does not appear in upcoming active list
        mockMvc.perform(get("/api/v1/recurring/upcoming?days=7")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + recurringId + ")]", hasSize(0)));

        // 5. Resume recurrence
        mockMvc.perform(patch("/api/v1/recurring/" + recurringId + "/resume")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    @DisplayName("Safe Failure Handling: Insufficient funds must NOT silently deduct money from bank accounts")
    void testSafeFailureHandlingInsufficientFunds() throws Exception {
        // Account has balance ₹50,000.00. Configure recurring bill for ₹999,999.00
        BigDecimal largeAmount = new BigDecimal("999999.00");
        LocalDate today = LocalDate.now();

        RecurringTransactionRequest request = RecurringTransactionRequest.builder()
                .title("Luxury Villa Maintenance")
                .accountId(accountId)
                .categoryId(categoryId)
                .amount(largeAmount)
                .transactionType(TransactionType.EXPENSE)
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(today)
                .nextExecutionDate(today) // Due today
                .autoPost(true)
                .description("Excessive recurring bill")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/recurring")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long recurringId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        BigDecimal balanceBefore = accountRepository.findById(accountId).orElseThrow().getCurrentBalance();
        long txCountBefore = transactionRepository.count();

        // Trigger processing of due recurring payments
        int processedCount = recurringService.processDueRecurringTransactions();
        assertThat(processedCount).isEqualTo(0); // Failed safely, 0 processed

        // CRITICAL INVARIANT: Money was NOT deducted from bank account!
        BigDecimal balanceAfter = accountRepository.findById(accountId).orElseThrow().getCurrentBalance();
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore);
        assertThat(balanceAfter).isEqualByComparingTo(new BigDecimal("50000.00"));

        // No transaction was created
        long txCountAfter = transactionRepository.count();
        assertThat(txCountAfter).isEqualTo(txCountBefore);

        // System notification generated alerting user of payment failure due to insufficient funds
        boolean alertFound = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getTitle().contains("Scheduled Payment Failed") &&
                               n.getMessage().contains("insufficient balance"));
        assertThat(alertFound).isTrue();
    }

    @Test
    @DisplayName("Prevent Duplicate Scheduled Transactions: Same cycle execution runs once and steps forward")
    void testPreventDuplicateScheduledTransactions() throws Exception {
        LocalDate today = LocalDate.now();

        RecurringTransactionRequest request = RecurringTransactionRequest.builder()
                .title("Netflix Premium Subscription")
                .accountId(accountId)
                .categoryId(categoryId)
                .amount(new BigDecimal("649.00"))
                .transactionType(TransactionType.EXPENSE)
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(today)
                .nextExecutionDate(today) // Due today
                .autoPost(true)
                .description("4K Streaming subscription")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/recurring")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long recurringId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // First execution run: Should succeed and deduct ₹649.00
        int processedFirstRun = recurringService.processDueRecurringTransactions();
        assertThat(processedFirstRun).isGreaterThanOrEqualTo(1);

        BigDecimal balanceAfterFirst = accountRepository.findById(accountId).orElseThrow().getCurrentBalance();
        assertThat(balanceAfterFirst).isEqualByComparingTo(new BigDecimal("49351.00"));

        // Verify transaction was generated with recurringTransaction link
        String expectedRef = "REC-" + recurringId + "-" + today;
        boolean txExists = transactionRepository.findAll().stream()
                .anyMatch(t -> expectedRef.equals(t.getReferenceNumber()));
        assertThat(txExists).isTrue();

        // Verify next execution date was advanced to next month
        LocalDate nextMonth = today.plusMonths(1);
        var recurringEntity = recurringRepository.findById(recurringId).orElseThrow();
        assertThat(recurringEntity.getNextExecutionDate()).isEqualTo(nextMonth);

        // Second execution run on same day: Must NOT create duplicate transaction or deduct again!
        int processedSecondRun = recurringService.processDueRecurringTransactions();
        assertThat(processedSecondRun).isEqualTo(0);

        BigDecimal balanceAfterSecond = accountRepository.findById(accountId).orElseThrow().getCurrentBalance();
        assertThat(balanceAfterSecond).isEqualByComparingTo(balanceAfterFirst);
    }
}
