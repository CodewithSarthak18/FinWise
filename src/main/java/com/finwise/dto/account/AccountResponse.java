package com.finwise.dto.account;

import com.finwise.entity.AccountStatus;
import com.finwise.entity.AccountType;
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
public class AccountResponse {

    private Long id;
    private String accountName;
    private AccountType accountType;
    private String institutionName;
    private String accountNumberMask;
    private BigDecimal currentBalance;
    private BigDecimal initialBalance;
    private BigDecimal creditLimit;
    private String currencyCode;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
