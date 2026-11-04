package com.finwise.service.impl;

import com.finwise.dto.goal.GoalContributionRequest;
import com.finwise.dto.goal.GoalContributionResponse;
import com.finwise.dto.goal.GoalRequest;
import com.finwise.dto.goal.GoalResponse;
import com.finwise.entity.*;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.InsufficientFundsException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.mapper.GoalMapper;
import com.finwise.repository.AccountRepository;
import com.finwise.repository.GoalContributionRepository;
import com.finwise.repository.NotificationRepository;
import com.finwise.repository.SavingsGoalRepository;
import com.finwise.repository.UserRepository;
import com.finwise.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final GoalMapper goalMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(Long userId) {
        return goalRepository.findByUserIdOrderByTargetDateAsc(userId).stream()
                .map(goalMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long goalId, Long userId) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", "id", goalId));
        return goalMapper.toResponse(goal);
    }

    @Override
    @Transactional
    public GoalResponse createGoal(GoalRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        SavingsGoal goal = goalMapper.toEntity(request, user);
        SavingsGoal saved = goalRepository.save(goal);
        return goalMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long goalId, GoalRequest request, Long userId) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", "id", goalId));

        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        if (request.getPriority() != null) {
            goal.setPriority(request.getPriority());
        }

        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        } else if (goal.getStatus() == GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }

        SavingsGoal updated = goalRepository.save(goal);
        return goalMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public GoalResponse contributeToGoal(Long goalId, GoalContributionRequest request, Long userId) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", "id", goalId));

        if (goal.getStatus() == GoalStatus.COMPLETED) {
            throw new BadRequestException("This savings goal is already fully achieved!");
        }

        Account account = accountRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        BigDecimal amount = request.getAmount();

        if (account.getAccountType() != AccountType.CREDIT_CARD && account.getCurrentBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getAccountName(), account.getCurrentBalance(), amount);
        }

        // Debit funding account
        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        accountRepository.save(account);

        // Credit goal balance
        BigDecimal newCurrent = goal.getCurrentAmount().add(amount);
        goal.setCurrentAmount(newCurrent);

        boolean reached = false;
        if (newCurrent.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
            reached = true;
        }

        goalRepository.save(goal);

        GoalContribution contribution = GoalContribution.builder()
                .savingsGoal(goal)
                .account(account)
                .amount(amount)
                .contributionDate(Instant.now())
                .note(request.getNote())
                .build();

        contributionRepository.save(contribution);

        // Record notifications
        Notification contributionNotification = Notification.builder()
                .user(goal.getUser())
                .title("Savings Goal Contribution")
                .message("Contributed ₹" + amount + " from " + account.getAccountName() + " to goal '" + goal.getTitle() + "'.")
                .type(NotificationType.GOAL_CONTRIBUTION)
                .actionUrl("/goals/" + goal.getId())
                .read(false)
                .build();
        notificationRepository.save(contributionNotification);

        if (reached) {
            Notification reachedNotification = Notification.builder()
                    .user(goal.getUser())
                    .title("Goal Achieved! 🎉")
                    .message("Congratulations! You have reached your savings goal '" + goal.getTitle() + "' with ₹" + newCurrent + " saved!")
                    .type(NotificationType.GOAL_REACHED)
                    .actionUrl("/goals/" + goal.getId())
                    .read(false)
                    .build();
            notificationRepository.save(reachedNotification);
        }

        return goalMapper.toResponse(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalContributionResponse> getGoalContributions(Long goalId, Long userId) {
        // Ensure ownership
        goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", "id", goalId));

        return contributionRepository.findBySavingsGoalIdOrderByContributionDateDesc(goalId).stream()
                .map(goalMapper::toContributionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteGoal(Long goalId, Long userId) {
        SavingsGoal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", "id", goalId));
        goalRepository.delete(goal);
    }
}
