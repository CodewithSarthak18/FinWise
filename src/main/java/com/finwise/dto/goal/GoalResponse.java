package com.finwise.dto.goal;

import com.finwise.entity.GoalPriority;
import com.finwise.entity.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private double progressPercentage;
    private LocalDate targetDate;
    private GoalStatus status;
    private GoalPriority priority;
    private Instant createdAt;
}
