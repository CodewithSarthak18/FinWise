package com.finwise.service;

import com.finwise.dto.goal.GoalContributionRequest;
import com.finwise.dto.goal.GoalContributionResponse;
import com.finwise.dto.goal.GoalRequest;
import com.finwise.dto.goal.GoalResponse;

import java.util.List;

public interface GoalService {

    List<GoalResponse> getGoals(Long userId);

    GoalResponse getGoalById(Long goalId, Long userId);

    GoalResponse createGoal(GoalRequest request, Long userId);

    GoalResponse updateGoal(Long goalId, GoalRequest request, Long userId);

    GoalResponse contributeToGoal(Long goalId, GoalContributionRequest request, Long userId);

    List<GoalContributionResponse> getGoalContributions(Long goalId, Long userId);

    void deleteGoal(Long goalId, Long userId);
}
