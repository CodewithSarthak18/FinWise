package com.finwise.service.impl;

import com.finwise.dto.response.PageResponse;
import com.finwise.dto.transaction.RefundRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.transaction.TransactionResponse;
import com.finwise.entity.*;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.DuplicateResourceException;
import com.finwise.exception.InsufficientFundsException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.exception.UnauthorizedException;
import com.finwise.mapper.TransactionMapper;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.TransactionRepository;
import com.finwise.repository.UserRepository;
import com.finwise.repository.specification.TransactionSpecification;
import com.finwise.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(
            Long userId,
            Long accountId,
            Long categoryId,
            TransactionType transactionType,
            Instant startDate,
            Instant endDate,
            String search,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable) {

        Specification<Transaction> spec = TransactionSpecification.withFilters(
                userId, accountId, categoryId, transactionType, startDate, endDate, search, minAmount, maxAmount);

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(transactionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));
        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        if (!category.isSystem() && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
            throw new UnauthorizedException("You do not have access to category with id: " + request.getCategoryId());
        }

        BigDecimal amount = request.getAmount();

        // Prevent duplicate processing via reference number
        if (request.getReferenceNumber() != null && !request.getReferenceNumber().isBlank()) {
            if (transactionRepository.existsByUserIdAndReferenceNumber(userId, request.getReferenceNumber().trim())) {
                throw new DuplicateResourceException("Transaction with reference number '" + request.getReferenceNumber() + "' already exists");
            }
        }

        if (request.getTransactionType() == TransactionType.EXPENSE) {
            if (account.getAccountType() != AccountType.CREDIT_CARD && account.getCurrentBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(account.getAccountName(), account.getCurrentBalance(), amount);
            }
            account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        } else if (request.getTransactionType() == TransactionType.INCOME || request.getTransactionType() == TransactionType.REFUND) {
            account.setCurrentBalance(account.getCurrentBalance().add(amount));
        } else {
            throw new BadRequestException("Direct transactions must be INCOME, EXPENSE, or REFUND. Use /transfers endpoint for account transfers.");
        }

        accountRepository.save(account);

        Transaction transaction = transactionMapper.toEntity(request, user, account, category);
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionRequest request, Long userId) {
        Transaction existing = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        if (existing.getTransactionType() == TransactionType.TRANSFER_DEBIT ||
            existing.getTransactionType() == TransactionType.TRANSFER_CREDIT) {
            throw new BadRequestException("Transfer transactions cannot be edited directly. Edit the transfer record.");
        }

        Account oldAccount = existing.getAccount();
        BigDecimal oldAmount = existing.getAmount();
        TransactionType oldType = existing.getTransactionType();

        // 1. Revert previous balance impact on old account
        if (oldType == TransactionType.EXPENSE) {
            oldAccount.setCurrentBalance(oldAccount.getCurrentBalance().add(oldAmount));
        } else if (oldType == TransactionType.INCOME || oldType == TransactionType.REFUND) {
            oldAccount.setCurrentBalance(oldAccount.getCurrentBalance().subtract(oldAmount));
        }

        // 2. Fetch new account (or reuse old account)
        Account newAccount = oldAccount;
        if (!request.getAccountId().equals(oldAccount.getId())) {
            accountRepository.save(oldAccount); // Persist reverted balance on old account
            newAccount = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getAccountId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));
        }

        // 3. Fetch new category and verify ownership
        Category newCategory = categoryRepository.findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        if (!newCategory.isSystem() && (newCategory.getUser() == null || !newCategory.getUser().getId().equals(userId))) {
            throw new UnauthorizedException("You do not have access to category with id: " + request.getCategoryId());
        }

        BigDecimal newAmount = request.getAmount();
        TransactionType newType = request.getTransactionType();

        // 4. Apply new balance impact on new account
        if (newType == TransactionType.EXPENSE) {
            if (newAccount.getAccountType() != AccountType.CREDIT_CARD && newAccount.getCurrentBalance().compareTo(newAmount) < 0) {
                throw new InsufficientFundsException(newAccount.getAccountName(), newAccount.getCurrentBalance(), newAmount);
            }
            newAccount.setCurrentBalance(newAccount.getCurrentBalance().subtract(newAmount));
        } else if (newType == TransactionType.INCOME || newType == TransactionType.REFUND) {
            newAccount.setCurrentBalance(newAccount.getCurrentBalance().add(newAmount));
        } else {
            throw new BadRequestException("Transaction type must be INCOME, EXPENSE, or REFUND");
        }

        accountRepository.save(newAccount);

        // 5. Update transaction fields
        existing.setAccount(newAccount);
        existing.setCategory(newCategory);
        existing.setTransactionType(newType);
        existing.setAmount(newAmount);
        existing.setTransactionDate(request.getTransactionDate());
        existing.setMerchantName(request.getMerchantName());
        existing.setDescription(request.getDescription());
        existing.setNotes(request.getNotes());
        if (request.getPaymentMode() != null) {
            existing.setPaymentMode(request.getPaymentMode());
        }
        existing.setReferenceNumber(request.getReferenceNumber());
        existing.setReceiptUrl(request.getReceiptUrl());

        Transaction saved = transactionRepository.save(existing);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse refundTransaction(Long transactionId, RefundRequest request, Long userId) {
        Transaction original = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        if (original.getTransactionType() != TransactionType.EXPENSE) {
            throw new BadRequestException("Only EXPENSE transactions can be refunded. Transaction is " + original.getTransactionType());
        }

        String refMarker = "REFUND-" + original.getId();
        BigDecimal existingRefunds = transactionRepository.sumRefundsForReference(userId, refMarker);
        BigDecimal maxRefundable = original.getAmount().subtract(existingRefunds);

        if (maxRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This transaction has already been fully refunded.");
        }

        BigDecimal refundAmount = (request != null && request.getAmount() != null) ? request.getAmount() : maxRefundable;
        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw new BadRequestException(String.format(
                    "Requested refund of ₹%s exceeds maximum refundable balance of ₹%s",
                    refundAmount.toPlainString(), maxRefundable.toPlainString()));
        }

        Account account = original.getAccount();
        account.setCurrentBalance(account.getCurrentBalance().add(refundAmount));
        accountRepository.save(account);

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank()) ?
                request.getReason() : "Refund for: " + (original.getDescription() != null ? original.getDescription() : "Expense #" + original.getId());

        Transaction refundTx = Transaction.builder()
                .user(original.getUser())
                .account(account)
                .category(original.getCategory())
                .transactionType(TransactionType.REFUND)
                .amount(refundAmount)
                .currencyCode(original.getCurrencyCode())
                .transactionDate(Instant.now())
                .merchantName(original.getMerchantName())
                .description(reason)
                .notes("Original Transaction ID: #" + original.getId())
                .paymentMode(original.getPaymentMode())
                .referenceNumber(refMarker)
                .status(TransactionStatus.COMPLETED)
                .build();

        Transaction savedRefund = transactionRepository.save(refundTx);
        return transactionMapper.toResponse(savedRefund);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        if (transaction.getTransactionType() == TransactionType.TRANSFER_DEBIT ||
            transaction.getTransactionType() == TransactionType.TRANSFER_CREDIT) {
            throw new BadRequestException("Transfer transactions cannot be deleted directly. Delete the transfer record.");
        }

        Account account = transaction.getAccount();
        BigDecimal amount = transaction.getAmount();

        // Roll back the account balance
        if (transaction.getTransactionType() == TransactionType.EXPENSE) {
            account.setCurrentBalance(account.getCurrentBalance().add(amount));
        } else if (transaction.getTransactionType() == TransactionType.INCOME ||
                   transaction.getTransactionType() == TransactionType.REFUND) {
            account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        }

        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }
}
