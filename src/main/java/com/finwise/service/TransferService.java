package com.finwise.service;

import com.finwise.dto.response.PageResponse;
import com.finwise.dto.transaction.TransferRequest;
import com.finwise.dto.transaction.TransferResponse;
import org.springframework.data.domain.Pageable;

public interface TransferService {

    TransferResponse executeTransfer(TransferRequest request, Long userId);

    PageResponse<TransferResponse> getTransfers(Long userId, Pageable pageable);

    TransferResponse getTransferById(Long transferId, Long userId);

    void deleteTransfer(Long transferId, Long userId);
}
