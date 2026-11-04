package com.finwise.mapper;

import com.finwise.dto.goal.GoalContributionResponse;
import com.finwise.dto.goal.GoalRequest;
import com.finwise.dto.goal.GoalResponse;
import com.finwise.entity.GoalContribution;
import com.finwise.entity.GoalPriority;
import com.finwise.entity.GoalStatus;
import com.finwise.entity.SavingsGoal;
import com.finwise.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GoalMapper {

    public SavingsGoal toEntity(GoalRequest request, User user) {
        if (request == null) {
            return null;
        }

        return SavingsGoal.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.getTargetDate())
                .status(GoalStatus.IN_PROGRESS)
                .priority(request.getPriority() != null ? request.getPriority() : GoalPriority.MEDIUM)
                .build();
    }

    public GoalResponse toResponse(SavingsGoal goal) {
        if (goal == null) {
            return null;
        }

        BigDecimal target = goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount();
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);

        double progress = 0.0;
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            progress = current.multiply(new BigDecimal("100"))
                    .divide(target, 2, RoundingMode.HALF_EVEN)
                    .doubleValue();
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .targetAmount(target)
                .currentAmount(current)
                .remainingAmount(remaining)
                .progressPercentage(Math.min(progress, 100.0))
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus())
                .priority(goal.getPriority())
                .createdAt(goal.getCreatedAt())
                .build();
    }

    public GoalContributionResponse toContributionResponse(GoalContribution contribution) {
        if (contribution == null) {
            return null;
        }

        return GoalContributionResponse.builder()
                .id(contribution.getId())
                .goalId(contribution.getSavingsGoal() != null ? contribution.getSavingsGoal().getId() : null)
                .accountId(contribution.getAccount() != null ? contribution.getAccount().getId() : null)
                .accountName(contribution.getAccount() != null ? contribution.getAccount().getAccountName() : null)
                .amount(contribution.getAmount())
                .contributionDate(contribution.getContributionDate())
                .note(contribution.getNote())
                .createdAt(contribution.getCreatedAt())
                .build();
    }
}
