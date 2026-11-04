package com.finwise.service.impl;

import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.account.AccountResponse;
import com.finwise.entity.Account;
import com.finwise.entity.User;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.mapper.AccountMapper;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.UserRepository;
import com.finwise.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsForUser(Long userId) {
        return accountRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId, Long userId) {
        Account account = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse createAccount(AccountRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Account account = accountMapper.toEntity(request, user);
        Account savedAccount = accountRepository.save(account);
        return accountMapper.toResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(Long accountId, AccountRequest request, Long userId) {
        Account account = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        account.setAccountName(request.getAccountName());
        account.setAccountType(request.getAccountType());
        account.setInstitutionName(request.getInstitutionName());
        account.setAccountNumberMask(request.getAccountNumberMask());
        if (request.getCreditLimit() != null) {
            account.setCreditLimit(request.getCreditLimit());
        }

        Account updatedAccount = accountRepository.save(account);
        return accountMapper.toResponse(updatedAccount);
    }

    @Override
    @Transactional
    public void deleteAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        if (account.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException("Cannot delete account with a non-zero balance: ₹" +
                    account.getCurrentBalance().toPlainString() + ". Please transfer or adjust funds first.");
        }

        account.setDeletedAt(Instant.now());
        accountRepository.save(account);
    }
}
