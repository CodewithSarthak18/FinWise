package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.auth.PasswordChangeRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.dto.budget.BudgetCategoryRequest;
import com.finwise.dto.budget.BudgetRequest;
import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.transaction.RefundRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.transaction.TransferRequest;
import com.finwise.entity.*;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.RefreshTokenRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndFinancialAuditTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private String userAToken;
    private Long userAId;
    private Long userAAccountId;
    private Long userACategoryId;

    private String userBToken;
    private Long userBId;
    private Long userBAccountId;
    private Long userBCategoryId;

    @BeforeEach
    void setUp() throws Exception {
        String suffixA = "a" + System.nanoTime();
        String suffixB = "b" + (System.nanoTime() + 1);

        // Register User A
        RegisterRequest regA = RegisterRequest.builder()
                .firstName("Arjun")
                .lastName("Nair")
                .email("arjun." + suffixA + "@finwise.test")
                .password("SecureFinA#2026")
                .currencyCode("INR")
                .build();

        MvcResult resA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regA)))
                .andExpect(status().isCreated())
                .andReturn();

        userAToken = "Bearer " + objectMapper.readTree(resA.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        userAId = userRepository.findByEmail(regA.getEmail()).orElseThrow().getId();

        // Register User B
        RegisterRequest regB = RegisterRequest.builder()
                .firstName("Bhavna")
                .lastName("Patel")
                .email("bhavna." + suffixB + "@finwise.test")
                .password("SecureFinB#2026")
                .currencyCode("INR")
                .build();

        MvcResult resB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated())
                .andReturn();

        userBToken = "Bearer " + objectMapper.readTree(resB.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        userBId = userRepository.findByEmail(regB.getEmail()).orElseThrow().getId();

        // User A Account (₹50,000.00)
        AccountRequest accReqA = AccountRequest.builder()
                .accountName("Arjun HDFC Savings")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("50000.00"))
                .institutionName("HDFC")
                .build();

        MvcResult accResA = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReqA)))
                .andExpect(status().isCreated())
                .andReturn();
        userAAccountId = objectMapper.readTree(accResA.getResponse().getContentAsString()).path("data").path("id").asLong();

        // User B Account (₹20,000.00)
        AccountRequest accReqB = AccountRequest.builder()
                .accountName("Bhavna ICICI Savings")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("20000.00"))
                .institutionName("ICICI")
                .build();

        MvcResult accResB = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReqB)))
                .andExpect(status().isCreated())
                .andReturn();
        userBAccountId = objectMapper.readTree(accResB.getResponse().getContentAsString()).path("data").path("id").asLong();

        // User A Custom Private Category
        CategoryRequest catReqA = CategoryRequest.builder()
                .name("Arjun Private Gadgets " + suffixA)
                .type(CategoryType.EXPENSE)
                .colorCode("#00BCD4")
                .icon("laptop")
                .build();

        MvcResult catResA = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReqA)))
                .andExpect(status().isCreated())
                .andReturn();
        userACategoryId = objectMapper.readTree(catResA.getResponse().getContentAsString()).path("data").path("id").asLong();

        // User B Custom Private Category
        CategoryRequest catReqB = CategoryRequest.builder()
                .name("Bhavna Private Jewelry " + suffixB)
                .type(CategoryType.EXPENSE)
                .colorCode("#E91E63")
                .icon("gem")
                .build();

        MvcResult catResB = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReqB)))
                .andExpect(status().isCreated())
                .andReturn();
        userBCategoryId = objectMapper.readTree(catResB.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    // ==========================================
    // 1. SECURITY & CROSS-USER AUTHORIZATION
    // ==========================================

    @Test
    @DisplayName("Security Audit: User A cannot use User B's private category in transaction")
    void testCrossUserCategoryTransactionRejection() throws Exception {
        TransactionRequest maliciousReq = TransactionRequest.builder()
                .accountId(userAAccountId)
                .categoryId(userBCategoryId) // Target User B's private category!
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("1500.00"))
                .transactionDate(Instant.now())
                .description("Sneaky gadget purchase")
                .paymentMode(PaymentMode.UPI)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("You do not have access to category")));
    }

    @Test
    @DisplayName("Security Audit: User A cannot allocate User B's category in Budget")
    void testCrossUserCategoryBudgetRejection() throws Exception {
        BudgetRequest maliciousBudget = BudgetRequest.builder()
                .name("Arjun Sneaky Budget")
                .periodType(BudgetPeriodType.MONTHLY)
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1))
                .totalBudgetAmount(new BigDecimal("10000.00"))
                .categoryAllocations(List.of(
                        BudgetCategoryRequest.builder()
                                .categoryId(userBCategoryId) // Target User B's private category!
                                .allocatedAmount(new BigDecimal("5000.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousBudget)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("You do not have access to category")));
    }

    @Test
    @DisplayName("Security Audit: User A cannot nest category under User B's private category")
    void testCrossUserParentCategoryRejection() throws Exception {
        CategoryRequest subCatReq = CategoryRequest.builder()
                .name("Arjun SubCategory")
                .type(CategoryType.EXPENSE)
                .parentId(userBCategoryId) // Attempt to hijack User B's category as parent
                .colorCode("#FF9800")
                .icon("folder")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subCatReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("You do not have access to parent category")));
    }

    // ==========================================
    // 2. FINANCIAL LOGIC: REFUNDS
    // ==========================================

    @Test
    @DisplayName("Financial Logic: Full refund credits account balance and creates refund transaction")
    void testFullTransactionRefund() throws Exception {
        // 1. Create Expense of ₹4,000.00 from Arjun's account (₹50,000.00 -> ₹46,000.00)
        TransactionRequest expReq = TransactionRequest.builder()
                .accountId(userAAccountId)
                .categoryId(userACategoryId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("4000.00"))
                .transactionDate(Instant.now())
                .description("Noise cancelling headphones")
                .paymentMode(PaymentMode.CREDIT_CARD)
                .build();

        MvcResult expRes = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(expRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        Account accAfterExp = accountRepository.findById(userAAccountId).orElseThrow();
        assertThat(accAfterExp.getCurrentBalance()).isEqualByComparingTo("46000.00");

        // 2. Issue full refund of ₹4,000.00
        MvcResult refRes = mockMvc.perform(post("/api/v1/transactions/" + txId + "/refund")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefundRequest.builder()
                                .reason("Item returned - defective microphone")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.transactionType", is("REFUND")))
                .andExpect(jsonPath("$.data.amount", is("4000.00")))
                .andReturn();

        // 3. Verify Account balance is restored back to ₹50,000.00
        Account accAfterRefund = accountRepository.findById(userAAccountId).orElseThrow();
        assertThat(accAfterRefund.getCurrentBalance()).isEqualByComparingTo("50000.00");

        // 4. Attempting another refund on same transaction returns 400 Bad Request
        mockMvc.perform(post("/api/v1/transactions/" + txId + "/refund")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefundRequest.builder().amount(new BigDecimal("100.00")).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already been fully refunded")));
    }

    @Test
    @DisplayName("Financial Logic: Partial refund only credits requested partial amount")
    void testPartialTransactionRefund() throws Exception {
        // Expense of ₹10,000.00
        TransactionRequest expReq = TransactionRequest.builder()
                .accountId(userAAccountId)
                .categoryId(userACategoryId)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("10000.00"))
                .transactionDate(Instant.now())
                .description("Conference registration fee")
                .paymentMode(PaymentMode.NET_BANKING)
                .build();

        MvcResult expRes = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(expRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Partial refund of ₹3,000.00 (e.g. partial cancellation)
        mockMvc.perform(post("/api/v1/transactions/" + txId + "/refund")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefundRequest.builder()
                                .amount(new BigDecimal("3000.00"))
                                .reason("Early cancellation partial refund")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount", is("3000.00")));

        // Account balance was ₹50,000 - ₹10,000 + ₹3,000 = ₹43,000.00
        Account acc = accountRepository.findById(userAAccountId).orElseThrow();
        assertThat(acc.getCurrentBalance()).isEqualByComparingTo("43000.00");

        // Refunding ₹8,000 (which exceeds remaining ₹7,000) should fail
        mockMvc.perform(post("/api/v1/transactions/" + txId + "/refund")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefundRequest.builder()
                                .amount(new BigDecimal("8000.00"))
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds maximum refundable balance")));
    }

    // ==========================================
    // 3. FINANCIAL LOGIC: TRANSFERS & REVERSAL
    // ==========================================

    @Test
    @DisplayName("Financial Logic: Execute transfer and delete/revert transfer restoring balances")
    void testTransferExecutionAndReversal() throws Exception {
        // Create second account for User A
        AccountRequest acc2Req = AccountRequest.builder()
                .accountName("Arjun Fixed Savings")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("10000.00"))
                .institutionName("HDFC")
                .build();

        MvcResult acc2Res = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acc2Req)))
                .andExpect(status().isCreated())
                .andReturn();
        Long acc2Id = objectMapper.readTree(acc2Res.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Transfer ₹15,000.00 from Account 1 (₹50,000) to Account 2 (₹10,000)
        TransferRequest transferReq = TransferRequest.builder()
                .fromAccountId(userAAccountId)
                .toAccountId(acc2Id)
                .amount(new BigDecimal("15000.00"))
                .transferFee(new BigDecimal("20.00"))
                .transferDate(Instant.now())
                .note("Savings rebalance")
                .build();

        MvcResult txferRes = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long transferId = objectMapper.readTree(txferRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Balances:
        // Account 1: ₹50,000 - ₹15,020 = ₹34,980.00
        // Account 2: ₹10,000 + ₹15,000 = ₹25,000.00
        Account acc1After = accountRepository.findById(userAAccountId).orElseThrow();
        Account acc2After = accountRepository.findById(acc2Id).orElseThrow();
        assertThat(acc1After.getCurrentBalance()).isEqualByComparingTo("34980.00");
        assertThat(acc2After.getCurrentBalance()).isEqualByComparingTo("25000.00");

        // User B cannot delete User A's transfer
        mockMvc.perform(delete("/api/v1/transfers/" + transferId)
                        .header("Authorization", userBToken))
                .andExpect(status().isNotFound());

        // Revert Transfer via DELETE /api/v1/transfers/{id}
        mockMvc.perform(delete("/api/v1/transfers/" + transferId)
                        .header("Authorization", userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Balances restored:
        // Account 1 restored to ₹50,000.00
        // Account 2 restored to ₹10,000.00
        Account acc1Reverted = accountRepository.findById(userAAccountId).orElseThrow();
        Account acc2Reverted = accountRepository.findById(acc2Id).orElseThrow();
        assertThat(acc1Reverted.getCurrentBalance()).isEqualByComparingTo("50000.00");
        assertThat(acc2Reverted.getCurrentBalance()).isEqualByComparingTo("10000.00");
    }

    // ==========================================
    // 4. SECURITY: PASSWORD CHANGE & SESSION REVOCATION
    // ==========================================

    @Test
    @DisplayName("Security: Password change prevents same password and revokes active refresh tokens")
    void testPasswordChangeSecurityAndTokenRevocation() throws Exception {
        // Attempting to change to same password fails
        PasswordChangeRequest samePassReq = PasswordChangeRequest.builder()
                .currentPassword("SecureFinA#2026")
                .newPassword("SecureFinA#2026")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samePassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("New password cannot be the same as current password")));

        // Valid password change
        PasswordChangeRequest validReq = PasswordChangeRequest.builder()
                .currentPassword("SecureFinA#2026")
                .newPassword("BrandNewSecurePass#2027")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk());

        // Verify all refresh tokens for User A are revoked
        User userA = userRepository.findById(userAId).orElseThrow();
        long activeTokens = refreshTokenRepository.findByUser(userA).stream()
                .filter(t -> !t.isRevoked())
                .count();
        assertThat(activeTokens).isZero();
    }
}
