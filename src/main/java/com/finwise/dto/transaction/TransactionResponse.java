package com.finwise.dto.transaction;

import com.finwise.entity.PaymentMode;
import com.finwise.entity.TransactionStatus;
import com.finwise.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currencyCode;
    private Instant transactionDate;
    private String merchantName;
    private String description;
    private String notes;
    private PaymentMode paymentMode;
    private String referenceNumber;
    private String receiptUrl;
    private TransactionStatus status;
    private Instant createdAt;
}
