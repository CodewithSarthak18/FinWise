package com.finwise.service.impl;

import com.finwise.dto.recurring.RecurringTransactionRequest;
import com.finwise.dto.recurring.RecurringTransactionResponse;
import com.finwise.dto.recurring.UpcomingReminderResponse;
import com.finwise.entity.*;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.InsufficientFundsException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.exception.UnauthorizedException;
import com.finwise.mapper.RecurringTransactionMapper;
import com.finwise.repository.*;
import com.finwise.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionServiceImpl.class);

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final RecurringTransactionMapper recurringMapper;

    @Override
    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(RecurringTransactionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        if (!category.isSystem() && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
            throw new UnauthorizedException("You do not have access to this category");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        RecurringTransaction entity = recurringMapper.toEntity(request, user, account, category);
        RecurringTransaction saved = recurringRepository.save(entity);
        log.info("Created recurring transaction '{}' (id: {}) for user id: {}", saved.getTitle(), saved.getId(), userId);
        return recurringMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getRecurringTransactions(Long userId) {
        return recurringRepository.findByUserIdOrderByNextExecutionDateAsc(userId).stream()
                .map(recurringMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransactionById(Long id, Long userId) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring Transaction", "id", id));
        return recurringMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(Long id, RecurringTransactionRequest request, Long userId) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring Transaction", "id", id));

        Account account = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        if (!category.isSystem() && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
            throw new UnauthorizedException("You do not have access to this category");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        entity.setTitle(request.getTitle());
        entity.setAccount(account);
        entity.setCategory(category);
        entity.setAmount(request.getAmount());
        entity.setTransactionType(request.getTransactionType());
        entity.setFrequency(request.getFrequency());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        if (request.getNextExecutionDate() != null) {
            entity.setNextExecutionDate(request.getNextExecutionDate());
        }
        if (request.getAutoPost() != null) {
            entity.setAutoPost(request.getAutoPost());
        }
        entity.setDescription(request.getDescription());

        RecurringTransaction updated = recurringRepository.save(entity);
        log.info("Updated recurring transaction id: {} for user id: {}", id, userId);
        return recurringMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRecurringTransaction(Long id, Long userId) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring Transaction", "id", id));
        recurringRepository.delete(entity);
        log.info("Deleted recurring transaction id: {} for user id: {}", id, userId);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse pauseRecurringTransaction(Long id, Long userId) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring Transaction", "id", id));

        entity.setActive(false);
        RecurringTransaction updated = recurringRepository.save(entity);
        log.info("Paused recurring transaction id: {} for user id: {}", id, userId);
        return recurringMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse resumeRecurringTransaction(Long id, Long userId) {
        RecurringTransaction entity = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring Transaction", "id", id));

        entity.setActive(true);
        LocalDate today = LocalDate.now();

        // Step nextExecutionDate forward if it is in the past
        LocalDate nextDate = entity.getNextExecutionDate();
        while (nextDate.isBefore(today)) {
            nextDate = calculateNextExecutionDate(nextDate, entity.getFrequency());
        }

        if (entity.getEndDate() != null && nextDate.isAfter(entity.getEndDate())) {
            entity.setActive(false);
        } else {
            entity.setNextExecutionDate(nextDate);
        }

        RecurringTransaction updated = recurringRepository.save(entity);
        log.info("Resumed recurring transaction id: {} for user id: {}, next date: {}", id, userId, nextDate);
        return recurringMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingReminderResponse> getUpcomingRecurring(Long userId, int daysAhead) {
        int window = daysAhead > 0 ? daysAhead : 7;
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(window);

        return recurringRepository.findByUserIdAndActiveTrueAndNextExecutionDateBetweenOrderByNextExecutionDateAsc(
                        userId, today, futureDate)
                .stream()
                .map(recurringMapper::toReminderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LocalDate calculateNextExecutionDate(LocalDate currentDate, RecurringFrequency frequency) {
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        if (frequency == null) {
            frequency = RecurringFrequency.MONTHLY;
        }

        return switch (frequency) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case BI_WEEKLY -> currentDate.plusWeeks(2);
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case YEARLY -> currentDate.plusYears(1);
        };
    }

    @Override
    @Transactional
    public int processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueTransactions = recurringRepository
                .findByActiveTrueAndNextExecutionDateLessThanEqual(today);

        log.info("Found {} active recurring transactions due for processing as of {}", dueTransactions.size(), today);
        int processedCount = 0;

        for (RecurringTransaction recurring : dueTransactions) {
            try {
                boolean success = processSingleRecurringTransaction(recurring, today);
                if (success) {
                    processedCount++;
                }
            } catch (Exception ex) {
                log.error("Unexpected error processing recurring transaction id {}: {}", recurring.getId(), ex.getMessage(), ex);
            }
        }

        return processedCount;
    }

    @Override
    @Transactional
    public RecurringTransactionResponse executeRecurringTransaction(Long id, Long userId) {
        RecurringTransaction recurring = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring Transaction", "id", id));

        Account account = recurring.getAccount();
        BigDecimal amount = recurring.getAmount();

        if (recurring.getTransactionType() == TransactionType.EXPENSE) {
            if (account.getAccountType() != AccountType.CREDIT_CARD && account.getCurrentBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(account.getAccountName(), account.getCurrentBalance(), amount);
            }
        }

        processPaymentAndAdvanceDate(recurring, recurring.getNextExecutionDate());
        RecurringTransaction updated = recurringRepository.save(recurring);
        return recurringMapper.toResponse(updated);
    }

    private boolean processSingleRecurringTransaction(RecurringTransaction recurring, LocalDate executionDate) {
        // 1. Check end date
        if (recurring.getEndDate() != null && executionDate.isAfter(recurring.getEndDate())) {
            log.info("Recurring transaction id {} has passed its end date {}. Marking inactive.",
                    recurring.getId(), recurring.getEndDate());
            recurring.setActive(false);
            recurringRepository.save(recurring);
            return false;
        }

        // 2. Prevent duplicate scheduled execution using deterministic reference number
        String refNumber = "REC-" + recurring.getId() + "-" + recurring.getNextExecutionDate();
        if (transactionRepository.existsByUserIdAndReferenceNumber(recurring.getUser().getId(), refNumber)) {
            log.warn("Duplicate scheduled transaction prevented for recurring id {} on {}. Advancing next execution date.",
                    recurring.getId(), recurring.getNextExecutionDate());
            advanceRecurringDate(recurring);
            recurringRepository.save(recurring);
            return false;
        }

        // 3. Handle non-auto-post reminder
        if (!recurring.isAutoPost()) {
            Notification reminderNotification = Notification.builder()
                    .user(recurring.getUser())
                    .title("Upcoming Payment Due: " + recurring.getTitle())
                    .message("Reminder: Your recurring payment for '" + recurring.getTitle() + "' (₹" + recurring.getAmount() +
                            ") is due today from account '" + recurring.getAccount().getAccountName() + "'.")
                    .type(NotificationType.RECURRING_DUE)
                    .actionUrl("/recurring/" + recurring.getId())
                    .read(false)
                    .build();
            notificationRepository.save(reminderNotification);

            advanceRecurringDate(recurring);
            recurringRepository.save(recurring);
            return true;
        }

        // 4. Safe failure handling: Verify funds before deducting money from bank accounts
        Account account = recurring.getAccount();
        BigDecimal amount = recurring.getAmount();

        if (recurring.getTransactionType() == TransactionType.EXPENSE) {
            if (account.getAccountType() != AccountType.CREDIT_CARD && account.getCurrentBalance().compareTo(amount) < 0) {
                // INSUFFICIENT FUNDS: DO NOT SILENTLY DEDUCT MONEY
                log.warn("[SAFE_PAYMENT_FAILURE] Insufficient balance in account '{}' for recurring payment '{}'. Available: ₹{}, Required: ₹{}. Money was NOT deducted.",
                        account.getAccountName(), recurring.getTitle(), account.getCurrentBalance(), amount);

                Notification failureNotification = Notification.builder()
                        .user(recurring.getUser())
                        .title("Scheduled Payment Failed: " + recurring.getTitle())
                        .message("Automatic payment of ₹" + amount + " for '" + recurring.getTitle() +
                                "' could not be processed due to insufficient balance in " + account.getAccountName() +
                                " (Available: ₹" + account.getCurrentBalance() + "). No money was deducted.")
                        .type(NotificationType.SYSTEM_ALERT)
                        .actionUrl("/recurring/" + recurring.getId())
                        .read(false)
                        .build();
                notificationRepository.save(failureNotification);

                // Advance next execution date so it does not repeatedly fail every minute
                advanceRecurringDate(recurring);
                recurringRepository.save(recurring);
                return false;
            }
        }

        // 5. Sufficient funds: Execute payment safely
        processPaymentAndAdvanceDate(recurring, executionDate);
        recurringRepository.save(recurring);
        return true;
    }

    private void processPaymentAndAdvanceDate(RecurringTransaction recurring, LocalDate cycleDate) {
        Account account = recurring.getAccount();
        BigDecimal amount = recurring.getAmount();
        String refNumber = "REC-" + recurring.getId() + "-" + cycleDate;

        // Maintain account balance correctly
        if (recurring.getTransactionType() == TransactionType.EXPENSE) {
            account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        } else if (recurring.getTransactionType() == TransactionType.INCOME) {
            account.setCurrentBalance(account.getCurrentBalance().add(amount));
        }
        accountRepository.save(account);

        // Record completed transaction
        Transaction transaction = Transaction.builder()
                .user(recurring.getUser())
                .account(account)
                .category(recurring.getCategory())
                .recurringTransaction(recurring)
                .transactionType(recurring.getTransactionType())
                .amount(amount)
                .currencyCode(account.getCurrencyCode() != null ? account.getCurrencyCode() : "INR")
                .transactionDate(Instant.now())
                .merchantName(recurring.getTitle())
                .description(recurring.getDescription() != null ? recurring.getDescription() : "Recurring payment: " + recurring.getTitle())
                .notes("Auto-scheduled execution for cycle date: " + cycleDate)
                .paymentMode(PaymentMode.NET_BANKING)
                .referenceNumber(refNumber)
                .status(TransactionStatus.COMPLETED)
                .build();

        transactionRepository.save(transaction);

        // Notify user of successful payment
        Notification successNotification = Notification.builder()
                .user(recurring.getUser())
                .title("Recurring Payment Processed")
                .message("Recurring payment '" + recurring.getTitle() + "' of ₹" + amount +
                        " was successfully processed from " + account.getAccountName() + ".")
                .type(NotificationType.RECURRING_DUE)
                .actionUrl("/transactions/" + transaction.getId())
                .read(false)
                .build();
        notificationRepository.save(successNotification);

        // Advance to next cycle date
        advanceRecurringDate(recurring);
    }

    private void advanceRecurringDate(RecurringTransaction recurring) {
        LocalDate nextDate = calculateNextExecutionDate(recurring.getNextExecutionDate(), recurring.getFrequency());
        recurring.setNextExecutionDate(nextDate);

        if (recurring.getEndDate() != null && nextDate.isAfter(recurring.getEndDate())) {
            log.info("Recurring payment id {} reached its end date {}. Inactivating.", recurring.getId(), recurring.getEndDate());
            recurring.setActive(false);
        }
    }
}
