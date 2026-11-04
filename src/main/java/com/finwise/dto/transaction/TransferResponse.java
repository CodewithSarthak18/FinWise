package com.finwise.dto.transaction;

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
public class TransferResponse {

    private Long id;
    private Long fromAccountId;
    private String fromAccountName;
    private Long toAccountId;
    private String toAccountName;
    private Long debitTransactionId;
    private Long creditTransactionId;
    private BigDecimal amount;
    private BigDecimal transferFee;
    private Instant transferDate;
    private String note;
    private Instant createdAt;
}
