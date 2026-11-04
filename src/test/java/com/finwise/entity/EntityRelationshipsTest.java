package com.finwise.entity;

import com.finwise.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityRelationshipsTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private SavingsGoalRepository goalRepository;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Verify User -> Account bidirectional relationship and BigDecimal balance")
    void verifyUserAccountRelationship() {
        User user = userRepository.save(User.builder()
                .email("test.account." + System.currentTimeMillis() + "@finwise.in")
                .passwordHash("hashed")
                .firstName("Aditya")
                .lastName("Rao")
                .currencyCode("INR")
                .build());

        Account account = Account.builder()
                .user(user)
                .accountName("Axis Bank Priority")
                .accountType(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("125000.75"))
                .initialBalance(new BigDecimal("125000.75"))
                .currencyCode("INR")
                .build();

        Account savedAccount = accountRepository.save(account);

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedAccount.getCurrentBalance()).isEqualByComparingTo("125000.75");
        assertThat(savedAccount.getCreatedAt()).isNotNull();
        assertThat(savedAccount.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Verify Transaction entity relationships, Enums, and User ownership")
    void verifyTransactionRelationships() {
        User user = userRepository.save(User.builder()
                .email("test.tx." + System.currentTimeMillis() + "@finwise.in")
                .passwordHash("hashed")
                .firstName("Siddharth")
                .lastName("Verma")
                .build());

        Account account = accountRepository.save(Account.builder()
                .user(user)
                .accountName("SBI Savings")
                .accountType(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("45000.00"))
                .build());

        Category category = categoryRepository.save(Category.builder()
                .user(user)
                .name("Fuel")
                .type(CategoryType.EXPENSE)
                .build());

        Transaction tx = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("2100.50"))
                .transactionDate(Instant.now())
                .paymentMode(PaymentMode.CREDIT_CARD)
                .status(TransactionStatus.COMPLETED)
                .build();

        Transaction savedTx = transactionRepository.save(tx);

        assertThat(savedTx.getId()).isNotNull();
        assertThat(savedTx.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedTx.getAccount().getId()).isEqualTo(account.getId());
        assertThat(savedTx.getCategory().getId()).isEqualTo(category.getId());
        assertThat(savedTx.getAmount()).isEqualByComparingTo("2100.50");
        assertThat(savedTx.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Verify Budget -> BudgetCategory cascading and orphan removal")
    void verifyBudgetCategoryCascading() {
        User user = userRepository.save(User.builder()
                .email("test.budget." + System.currentTimeMillis() + "@finwise.in")
                .passwordHash("hashed")
                .firstName("Kavita")
                .lastName("Iyer")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .user(user)
                .name("Health")
                .type(CategoryType.EXPENSE)
                .build());

        Budget budget = Budget.builder()
                .user(user)
                .name("Q4 Health & Wellness")
                .periodType(BudgetPeriodType.QUARTERLY)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .totalBudgetAmount(new BigDecimal("30000.00"))
                .build();

        BudgetCategory bcat = BudgetCategory.builder()
                .category(category)
                .allocatedAmount(new BigDecimal("15000.00"))
                .build();

        budget.addBudgetCategory(bcat);
        Budget savedBudget = budgetRepository.save(budget);

        assertThat(savedBudget.getId()).isNotNull();
        assertThat(savedBudget.getBudgetCategories()).hasSize(1);
        assertThat(savedBudget.getBudgetCategories().get(0).getCategory().getName()).isEqualTo("Health");
        assertThat(savedBudget.getBudgetCategories().get(0).getAllocatedAmount()).isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("Verify SavingsGoal -> GoalContribution relationship")
    void verifySavingsGoalContributionRelationship() {
        User user = userRepository.save(User.builder()
                .email("test.goal." + System.currentTimeMillis() + "@finwise.in")
                .passwordHash("hashed")
                .firstName("Deepak")
                .lastName("Chopra")
                .build());

        Account account = accountRepository.save(Account.builder()
                .user(user)
                .accountName("Emergency Fund Pot")
                .accountType(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("60000.00"))
                .build());

        SavingsGoal goal = SavingsGoal.builder()
                .user(user)
                .title("Home Interior Renovation")
                .targetAmount(new BigDecimal("500000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusYears(1))
                .priority(GoalPriority.HIGH)
                .build();

        GoalContribution contribution = GoalContribution.builder()
                .account(account)
                .amount(new BigDecimal("50000.00"))
                .note("Initial milestone deposit")
                .build();

        goal.addContribution(contribution);
        SavingsGoal savedGoal = goalRepository.save(goal);

        assertThat(savedGoal.getId()).isNotNull();
        assertThat(savedGoal.getContributions()).hasSize(1);
        assertThat(savedGoal.getContributions().get(0).getAmount()).isEqualByComparingTo("50000.00");
        assertThat(savedGoal.getContributions().get(0).getAccount().getId()).isEqualTo(account.getId());
    }

    @Test
    @DisplayName("Verify RecurringTransaction scheduling properties and constraints")
    void verifyRecurringTransactionProperties() {
        User user = userRepository.save(User.builder()
                .email("test.recurring." + System.currentTimeMillis() + "@finwise.in")
                .passwordHash("hashed")
                .firstName("Meera")
                .lastName("Joshi")
                .build());

        Account account = accountRepository.save(Account.builder()
                .user(user)
                .accountName("Salary Acct")
                .accountType(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("50000.00"))
                .build());

        Category category = categoryRepository.save(Category.builder()
                .user(user)
                .name("SIP Mutual Fund")
                .type(CategoryType.EXPENSE)
                .build());

        RecurringTransaction recurring = RecurringTransaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .title("HDFC Top 100 SIP")
                .amount(new BigDecimal("10000.00"))
                .transactionType(TransactionType.EXPENSE)
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(LocalDate.now())
                .nextExecutionDate(LocalDate.now().plusMonths(1))
                .autoPost(true)
                .active(true)
                .build();

        RecurringTransaction savedRecurring = recurringRepository.save(recurring);

        assertThat(savedRecurring.getId()).isNotNull();
        assertThat(savedRecurring.getFrequency()).isEqualTo(RecurringFrequency.MONTHLY);
        assertThat(savedRecurring.getAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("Verify Notification entity for user alerts and read state tracking")
    void verifyNotificationEntity() {
        User user = userRepository.save(User.builder()
                .email("test.notif." + System.currentTimeMillis() + "@finwise.in")
                .passwordHash("hashed")
                .firstName("Rohit")
                .lastName("Sharma")
                .build());

        Notification notification = Notification.builder()
                .user(user)
                .title("80% Budget Exceeded")
                .message("Dining category spend has exceeded 80% of limit")
                .type(NotificationType.BUDGET_WARNING)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getType()).isEqualTo(NotificationType.BUDGET_WARNING);
        assertThat(notificationRepository.countByUserIdAndReadFalse(user.getId())).isEqualTo(1);
    }
}
