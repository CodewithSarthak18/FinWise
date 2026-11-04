package com.finwise.repository;

import com.finwise.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdOrderByStartDateDesc(Long userId);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.active = true " +
           "AND :currentDate BETWEEN b.startDate AND b.endDate")
    Optional<Budget> findActiveBudgetForDate(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate ORDER BY b.startDate DESC")
    List<Budget> findBudgetsOverlappingDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
