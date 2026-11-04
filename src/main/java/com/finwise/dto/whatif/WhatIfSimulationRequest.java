package com.finwise.dto.whatif;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfSimulationRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Reduction percentage is required")
    @DecimalMin(value = "0.01", message = "Reduction percentage must be greater than 0%")
    @DecimalMax(value = "100.00", message = "Reduction percentage cannot exceed 100%")
    private BigDecimal reductionPercentage;

    private String month; // Optional "YYYY-MM"
}
