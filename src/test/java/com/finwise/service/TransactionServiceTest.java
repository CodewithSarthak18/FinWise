package com.finwise.service;

import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.transaction.TransactionResponse;
import com.finwise.entity.*;
import com.finwise.exception.InsufficientFundsException;
import com.finwise.mapper.TransactionMapper;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.repository.UserRepository;
import com.finwise.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User testUser;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@finwise.in")
                .firstName("Arun")
                .lastName("Kumar")
                .currencyCode("INR")
                .build();

        testAccount = Account.builder()
                .id(10L)
                .user(testUser)
                .accountName("HDFC Salary")
                .accountType(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("25000.00"))
                .initialBalance(new BigDecimal("25000.00"))
                .currencyCode("INR")
                .status(AccountStatus.ACTIVE)
                .build();

        testCategory = Category.builder()
                .id(5L)
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .system(true)
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("Should successfully record Expense transaction and deduct account balance with BigDecimal precision")
    void shouldCreateExpenseTransactionSuccessfully() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(10L)
                .categoryId(5L)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("3500.50"))
                .transactionDate(Instant.now())
                .merchantName("Supermarket")
                .build();

        Transaction mockEntity = Transaction.builder()
                .id(100L)
                .amount(new BigDecimal("3500.50"))
                .build();

        TransactionResponse mockResponse = TransactionResponse.builder()
                .id(100L)
                .amount(new BigDecimal("3500.50"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByIdAndUserIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(any(), any(), any(), any())).thenReturn(mockEntity);
        when(transactionRepository.save(any())).thenReturn(mockEntity);
        when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

        TransactionResponse result = transactionService.createTransaction(request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("3500.50");
        assertThat(testAccount.getCurrentBalance()).isEqualByComparingTo("21499.50");
        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when expense exceeds available balance")
    void shouldThrowInsufficientFundsWhenBalanceTooLow() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(10L)
                .categoryId(5L)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("30000.00"))
                .transactionDate(Instant.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByIdAndUserIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(testCategory));

        assertThatThrownBy(() -> transactionService.createTransaction(request, 1L))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("insufficient balance");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should increase balance when recording Income transaction")
    void shouldIncreaseBalanceForIncome() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(10L)
                .categoryId(5L)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("15000.00"))
                .transactionDate(Instant.now())
                .build();

        Transaction mockEntity = Transaction.builder().id(101L).build();
        TransactionResponse mockResponse = TransactionResponse.builder().id(101L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByIdAndUserIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(testCategory));
        when(transactionMapper.toEntity(any(), any(), any(), any())).thenReturn(mockEntity);
        when(transactionRepository.save(any())).thenReturn(mockEntity);
        when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

        transactionService.createTransaction(request, 1L);

        assertThat(testAccount.getCurrentBalance()).isEqualByComparingTo("40000.00");
    }

    @Test
    @DisplayName("Should update transaction and recalculate balance correctly")
    void shouldUpdateTransactionAndRecalculateBalance() {
        // Initial transaction was an EXPENSE of ₹2,000.00
        Transaction existingTx = Transaction.builder()
                .id(200L)
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(Instant.now())
                .build();

        // Updating expense to ₹3,500.00 (additional ₹1,500 should be debited)
        TransactionRequest updateReq = TransactionRequest.builder()
                .accountId(10L)
                .categoryId(5L)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("3500.00"))
                .transactionDate(Instant.now())
                .description("Updated grocery expense")
                .build();

        when(transactionRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(existingTx));
        when(categoryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any())).thenReturn(existingTx);
        when(transactionMapper.toResponse(any())).thenReturn(TransactionResponse.builder().amount(new BigDecimal("3500.00")).build());

        transactionService.updateTransaction(200L, updateReq, 1L);

        // Account balance was 25000.00. Reverting 2000 -> 27000.00. Debiting 3500 -> 23500.00.
        assertThat(testAccount.getCurrentBalance()).isEqualByComparingTo("23500.00");
    }

    @Test
    @DisplayName("Should prevent duplicate transaction when reference number already exists")
    void shouldPreventDuplicateTransactionWhenReferenceNumberExists() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(10L)
                .categoryId(5L)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .referenceNumber("UPI-REF-123456")
                .transactionDate(Instant.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findByIdAndUserIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.existsByUserIdAndReferenceNumber(1L, "UPI-REF-123456")).thenReturn(true);

        assertThatThrownBy(() -> transactionService.createTransaction(request, 1L))
                .isInstanceOf(com.finwise.exception.DuplicateResourceException.class)
                .hasMessageContaining("UPI-REF-123456");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should restore account balance when deleting an expense transaction")
    void shouldRestoreBalanceWhenDeletingExpense() {
        Transaction existingTx = Transaction.builder()
                .id(300L)
                .user(testUser)
                .account(testAccount)
                .category(testCategory)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("4000.00"))
                .transactionDate(Instant.now())
                .build();

        when(transactionRepository.findByIdAndUserId(300L, 1L)).thenReturn(Optional.of(existingTx));

        transactionService.deleteTransaction(300L, 1L);

        // Account balance was 25000.00. Deleting expense adds 4000.00 -> 29000.00
        assertThat(testAccount.getCurrentBalance()).isEqualByComparingTo("29000.00");
        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).delete(existingTx);
    }
}
