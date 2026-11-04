package com.finwise.repository;

import com.finwise.entity.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    @Query("SELECT c FROM GoalContribution c WHERE c.savingsGoal.id = :goalId ORDER BY c.contributionDate DESC")
    List<GoalContribution> findByGoalIdOrderByContributionDateDesc(@Param("goalId") Long goalId);

    List<GoalContribution> findBySavingsGoalIdOrderByContributionDateDesc(Long savingsGoalId);

    @Query("SELECT c FROM GoalContribution c WHERE c.savingsGoal.user.id = :userId " +
           "AND c.contributionDate >= :startDate AND c.contributionDate <= :endDate ORDER BY c.contributionDate DESC")
    List<GoalContribution> findByUserIdAndContributionDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") java.time.Instant startDate,
            @Param("endDate") java.time.Instant endDate);
}
