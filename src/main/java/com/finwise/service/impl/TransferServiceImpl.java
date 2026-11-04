package com.finwise.service.impl;

import com.finwise.dto.response.PageResponse;
import com.finwise.dto.transaction.TransferRequest;
import com.finwise.dto.transaction.TransferResponse;
import com.finwise.entity.*;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.InsufficientFundsException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.mapper.TransactionMapper;
import com.finwise.repository.*;
import com.finwise.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request, Long userId) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new BadRequestException("Source and destination accounts must be distinct");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account fromAccount = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getFromAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Source Account", "id", request.getFromAccountId()));

        Account toAccount = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getToAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination Account", "id", request.getToAccountId()));

        BigDecimal totalDebit = request.getAmount().add(request.getTransferFee() != null ? request.getTransferFee() : BigDecimal.ZERO);

        if (fromAccount.getAccountType() != AccountType.CREDIT_CARD && fromAccount.getCurrentBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException(fromAccount.getAccountName(), fromAccount.getCurrentBalance(), totalDebit);
        }

        // Adjust account balances
        fromAccount.setCurrentBalance(fromAccount.getCurrentBalance().subtract(totalDebit));
        toAccount.setCurrentBalance(toAccount.getCurrentBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Fetch or assign fallback transfer category
        Category transferCategory = categoryRepository.findAllAvailableForUser(userId).stream()
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Transfer")
                        .type(CategoryType.EXPENSE)
                        .icon("arrow-left-right")
                        .system(true)
                        .build()));

        // Create paired debit transaction
        Transaction debitTx = Transaction.builder()
                .user(user)
                .account(fromAccount)
                .category(transferCategory)
                .transactionType(TransactionType.TRANSFER_DEBIT)
                .amount(totalDebit)
                .currencyCode(fromAccount.getCurrencyCode())
                .transactionDate(request.getTransferDate())
                .merchantName("Transfer to " + toAccount.getAccountName())
                .description(request.getNote())
                .paymentMode(PaymentMode.NET_BANKING)
                .status(TransactionStatus.COMPLETED)
                .build();
        Transaction savedDebitTx = transactionRepository.save(debitTx);

        // Create paired credit transaction
        Transaction creditTx = Transaction.builder()
                .user(user)
                .account(toAccount)
                .category(transferCategory)
                .transactionType(TransactionType.TRANSFER_CREDIT)
                .amount(request.getAmount())
                .currencyCode(toAccount.getCurrencyCode())
                .transactionDate(request.getTransferDate())
                .merchantName("Transfer from " + fromAccount.getAccountName())
                .description(request.getNote())
                .paymentMode(PaymentMode.NET_BANKING)
                .status(TransactionStatus.COMPLETED)
                .build();
        Transaction savedCreditTx = transactionRepository.save(creditTx);

        // Link into Transfer entity
        Transfer transfer = Transfer.builder()
                .user(user)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .debitTransaction(savedDebitTx)
                .creditTransaction(savedCreditTx)
                .amount(request.getAmount())
                .transferFee(request.getTransferFee() != null ? request.getTransferFee() : BigDecimal.ZERO)
                .transferDate(request.getTransferDate())
                .note(request.getNote())
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);
        return transactionMapper.toTransferResponse(savedTransfer);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getTransfers(Long userId, Pageable pageable) {
        Page<Transfer> page = transferRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(transactionMapper::toTransferResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransferById(Long transferId, Long userId) {
        Transfer transfer = transferRepository.findByIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", "id", transferId));
        return transactionMapper.toTransferResponse(transfer);
    }

    @Override
    @Transactional
    public void deleteTransfer(Long transferId, Long userId) {
        Transfer transfer = transferRepository.findByIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", "id", transferId));

        Account fromAccount = transfer.getFromAccount();
        Account toAccount = transfer.getToAccount();
        BigDecimal totalDebit = transfer.getAmount().add(transfer.getTransferFee() != null ? transfer.getTransferFee() : BigDecimal.ZERO);

        // Verify destination account has enough funds to reverse the received transfer
        if (toAccount.getAccountType() != AccountType.CREDIT_CARD && toAccount.getCurrentBalance().compareTo(transfer.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Cannot revert transfer: destination account '" + toAccount.getAccountName() + "' has insufficient funds",
                    toAccount.getCurrentBalance(),
                    transfer.getAmount()
            );
        }

        // Revert balances
        fromAccount.setCurrentBalance(fromAccount.getCurrentBalance().add(totalDebit));
        toAccount.setCurrentBalance(toAccount.getCurrentBalance().subtract(transfer.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Capture associated transactions before deleting transfer
        Transaction debitTx = transfer.getDebitTransaction();
        Transaction creditTx = transfer.getCreditTransaction();

        // Delete transfer record first to remove foreign key references
        transferRepository.delete(transfer);
        transferRepository.flush();

        // Delete associated debit and credit transactions
        if (debitTx != null) {
            transactionRepository.delete(debitTx);
        }
        if (creditTx != null) {
            transactionRepository.delete(creditTx);
        }
    }
}
