package com.finwise.dto.transaction;

import com.finwise.entity.PaymentMode;
import com.finwise.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class TransactionRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Transaction type is required (INCOME or EXPENSE)")
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be strictly greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Transaction date is required")
    private Instant transactionDate;

    @Size(max = 150, message = "Merchant name must not exceed 150 characters")
    private String merchantName;

    private String description;

    private String notes;

    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.UPI;

    private String referenceNumber;

    private String receiptUrl;
}
