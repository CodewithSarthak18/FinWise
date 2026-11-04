package com.finwise.controller;

import com.finwise.dto.response.ApiResponse;
import com.finwise.dto.response.PageResponse;
import com.finwise.dto.transaction.RefundRequest;
import com.finwise.dto.transaction.TransactionRequest;
import com.finwise.dto.transaction.TransactionResponse;
import com.finwise.entity.TransactionType;
import com.finwise.security.UserPrincipal;
import com.finwise.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.debug("Fetching transactions for user id: {}, page: {}, search: '{}'",
                principal.getId(), pageable.getPageNumber(), search);

        PageResponse<TransactionResponse> response = transactionService.getTransactions(
                principal.getId(), accountId, categoryId, transactionType, startDate, endDate, search, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.debug("Fetching transaction id: {} for user id: {}", id, principal.getId());
        TransactionResponse response = transactionService.getTransactionById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Creating {} transaction of ₹{} for user id: {}",
                request.getTransactionType(), request.getAmount(), principal.getId());
        TransactionResponse response = transactionService.createTransaction(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction recorded successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Updating transaction id: {} for user id: {}", id, principal.getId());
        TransactionResponse response = transactionService.updateTransaction(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<TransactionResponse>> refundTransaction(
            @PathVariable Long id,
            @RequestBody(required = false) RefundRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Processing refund for transaction id: {} by user id: {}", id, principal.getId());
        TransactionResponse response = transactionService.refundTransaction(id, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Refund processed successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Deleting transaction id: {} for user id: {}", id, principal.getId());
        transactionService.deleteTransaction(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }
}
