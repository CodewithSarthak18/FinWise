package com.finwise.service;

import com.finwise.dto.response.PageResponse;
import com.finwise.dto.transaction.RefundRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.transaction.TransactionResponse;
import com.finwise.entity.TransactionType;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;

public interface TransactionService {

    PageResponse<TransactionResponse> getTransactions(
            Long userId,
            Long accountId,
            Long categoryId,
            TransactionType transactionType,
            Instant startDate,
            Instant endDate,
            String search,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable
    );

    TransactionResponse getTransactionById(Long transactionId, Long userId);

    TransactionResponse createTransaction(TransactionRequest request, Long userId);

    TransactionResponse updateTransaction(Long transactionId, TransactionRequest request, Long userId);

    TransactionResponse refundTransaction(Long transactionId, RefundRequest request, Long userId);

    void deleteTransaction(Long transactionId, Long userId);
}
