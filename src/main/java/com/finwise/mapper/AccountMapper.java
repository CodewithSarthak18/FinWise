package com.finwise.mapper;

import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.account.AccountResponse;
import com.finwise.entity.Account;
import com.finwise.entity.AccountStatus;
import com.finwise.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toEntity(AccountRequest request, User user) {
        if (request == null) {
            return null;
        }

        return Account.builder()
                .user(user)
                .accountName(request.getAccountName())
                .accountType(request.getAccountType())
                .institutionName(request.getInstitutionName())
                .accountNumberMask(request.getAccountNumberMask())
                .currentBalance(request.getInitialBalance())
                .initialBalance(request.getInitialBalance())
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : java.math.BigDecimal.ZERO)
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR")
                .status(AccountStatus.ACTIVE)
                .build();
    }

    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }

        return AccountResponse.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .accountType(account.getAccountType())
                .institutionName(account.getInstitutionName())
                .accountNumberMask(account.getAccountNumberMask())
                .currentBalance(account.getCurrentBalance())
                .initialBalance(account.getInitialBalance())
                .creditLimit(account.getCreditLimit())
                .currencyCode(account.getCurrencyCode())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
