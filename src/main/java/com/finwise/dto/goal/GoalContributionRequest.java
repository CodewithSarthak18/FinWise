package com.finwise.dto.goal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalContributionRequest {

    @NotNull(message = "Account ID is required to fund this contribution")
    private Long accountId;

    @NotNull(message = "Contribution amount is required")
    @Positive(message = "Contribution amount must be greater than zero")
    private BigDecimal amount;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;
}
