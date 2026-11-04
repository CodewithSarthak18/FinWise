package com.finwise.repository;

import com.finwise.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserIdOrderByNextExecutionDateAsc(Long userId);

    List<RecurringTransaction> findByUserIdAndActiveTrue(Long userId);

    List<RecurringTransaction> findByUserIdAndActiveTrueOrderByNextExecutionDateAsc(Long userId);

    List<RecurringTransaction> findByActiveTrueAndNextExecutionDateLessThanEqual(LocalDate date);

    List<RecurringTransaction> findByUserIdAndActiveTrueAndNextExecutionDateBetweenOrderByNextExecutionDateAsc(
            Long userId, LocalDate fromDate, LocalDate toDate);

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);
}
