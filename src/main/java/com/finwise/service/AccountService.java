package com.finwise.service;

import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.account.AccountResponse;

import java.util.List;

public interface AccountService {

    List<AccountResponse> getAccountsForUser(Long userId);

    AccountResponse getAccountById(Long accountId, Long userId);

    AccountResponse createAccount(AccountRequest request, Long userId);

    AccountResponse updateAccount(Long accountId, AccountRequest request, Long userId);

    void deleteAccount(Long accountId, Long userId);
}
