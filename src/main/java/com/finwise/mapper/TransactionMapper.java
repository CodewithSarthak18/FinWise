package com.finwise.mapper;

import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.transaction.TransactionResponse;
import com.finwise.dto.transaction.TransferResponse;
import com.finwise.entity.*;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toEntity(TransactionRequest request, User user, Account account, Category category) {
        if (request == null) {
            return null;
        }

        return Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .currencyCode(account.getCurrencyCode())
                .transactionDate(request.getTransactionDate())
                .merchantName(request.getMerchantName())
                .description(request.getDescription())
                .notes(request.getNotes())
                .paymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : PaymentMode.UPI)
                .referenceNumber(request.getReferenceNumber())
                .receiptUrl(request.getReceiptUrl())
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .accountName(transaction.getAccount().getAccountName())
                .categoryId(transaction.getCategory().getId())
                .categoryName(transaction.getCategory().getName())
                .categoryColor(transaction.getCategory().getColorCode())
                .categoryIcon(transaction.getCategory().getIcon())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .currencyCode(transaction.getCurrencyCode())
                .transactionDate(transaction.getTransactionDate())
                .merchantName(transaction.getMerchantName())
                .description(transaction.getDescription())
                .notes(transaction.getNotes())
                .paymentMode(transaction.getPaymentMode())
                .referenceNumber(transaction.getReferenceNumber())
                .receiptUrl(transaction.getReceiptUrl())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    public TransferResponse toTransferResponse(Transfer transfer) {
        if (transfer == null) {
            return null;
        }

        return TransferResponse.builder()
                .id(transfer.getId())
                .fromAccountId(transfer.getFromAccount().getId())
                .fromAccountName(transfer.getFromAccount().getAccountName())
                .toAccountId(transfer.getToAccount().getId())
                .toAccountName(transfer.getToAccount().getAccountName())
                .debitTransactionId(transfer.getDebitTransaction().getId())
                .creditTransactionId(transfer.getCreditTransaction().getId())
                .amount(transfer.getAmount())
                .transferFee(transfer.getTransferFee())
                .transferDate(transfer.getTransferDate())
                .note(transfer.getNote())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
