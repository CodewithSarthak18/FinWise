package com.finwise.controller;

import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.account.AccountResponse;
import com.finwise.dto.response.ApiResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Fetching all accounts for user id: {}", principal.getId());
        List<AccountResponse> accounts = accountService.getAccountsForUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching account id: {} for user id: {}", id, principal.getId());
        AccountResponse response = accountService.getAccountById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Creating account '{}' for user id: {}", request.getAccountName(), principal.getId());
        AccountResponse response = accountService.createAccount(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Updating account id: {} for user id: {}", id, principal.getId());
        AccountResponse response = accountService.updateAccount(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Soft deleting account id: {} for user id: {}", id, principal.getId());
        accountService.deleteAccount(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }
}
