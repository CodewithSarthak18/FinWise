package com.finwise.dto.budget;

import com.finwise.entity.BudgetPeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {

    @NotBlank(message = "Budget name is required")
    @Size(max = 100, message = "Budget name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Period type is required")
    private BudgetPeriodType periodType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Total budget amount is required")
    @Positive(message = "Total budget amount must be greater than zero")
    private BigDecimal totalBudgetAmount;

    @DecimalMin(value = "1.00", message = "Threshold must be at least 1%")
    @DecimalMax(value = "100.00", message = "Threshold must not exceed 100%")
    @Builder.Default
    private BigDecimal notificationThreshold = new BigDecimal("80.00");

    @Valid
    private List<BudgetCategoryRequest> categoryAllocations;
}
