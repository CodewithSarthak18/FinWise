package com.finwise.controller;

import com.finwise.dto.response.ApiResponse;
import com.finwise.dto.response.PageResponse;
import com.finwise.dto.transaction.TransferRequest;
import com.finwise.dto.transaction.TransferResponse;
import com.finwise.security.UserPrincipal;
import com.finwise.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private static final Logger log = LoggerFactory.getLogger(TransferController.class);
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> executeTransfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Executing transfer of ₹{} from account {} to account {} for user id: {}",
                request.getAmount(), request.getFromAccountId(), request.getToAccountId(), principal.getId());
        TransferResponse response = transferService.executeTransfer(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer executed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransferResponse>>> getTransfers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "transferDate", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Fetching transfers for user id: {}, page: {}", principal.getId(), pageable.getPageNumber());
        PageResponse<TransferResponse> response = transferService.getTransfers(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransferById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Fetching transfer id: {} for user id: {}", id, principal.getId());
        TransferResponse response = transferService.getTransferById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Deleting/reverting transfer id: {} by user id: {}", id, principal.getId());
        transferService.deleteTransfer(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Transfer reverted successfully", null));
    }
}
