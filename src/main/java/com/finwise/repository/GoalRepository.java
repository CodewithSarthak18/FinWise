package com.finwise.repository;

import com.finwise.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByTargetDateAsc(Long userId);

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);
}
