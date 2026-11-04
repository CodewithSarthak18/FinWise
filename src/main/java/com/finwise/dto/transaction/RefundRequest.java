package com.finwise.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @DecimalMin(value = "0.01", message = "Refund amount must be strictly greater than zero")
    private BigDecimal amount; // If null, defaults to full original transaction amount

    private String reason;
}
