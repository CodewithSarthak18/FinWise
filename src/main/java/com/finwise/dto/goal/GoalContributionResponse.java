package com.finwise.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalContributionResponse {

    private Long id;
    private Long goalId;
    private Long accountId;
    private String accountName;
    private BigDecimal amount;
    private Instant contributionDate;
    private String note;
    private Instant createdAt;
}
