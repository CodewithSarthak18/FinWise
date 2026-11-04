package com.finwise.repository;

import com.finwise.entity.Transaction;
import com.finwise.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndReferenceNumber(Long userId, String referenceNumber);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionType = :type AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumAmountByUserIdAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.transactionType = 'EXPENSE' " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumExpenseByCategoryIdAndDateRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.transactionType = 'REFUND' " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumRefundByCategoryIdAndDateRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionType = 'REFUND' AND t.referenceNumber = :originalReference")
    BigDecimal sumRefundsForReference(
            @Param("userId") Long userId,
            @Param("originalReference") String originalReference);

    @Query("SELECT t.category.name, t.category.colorCode, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionType = 'EXPENSE' AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "GROUP BY t.category.name, t.category.colorCode ORDER BY SUM(t.amount) DESC")
    List<Object[]> findCategoryExpenseBreakdown(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT t.category.id, t.category.name, t.category.colorCode, t.category.icon, SUM(t.amount), COUNT(t.id) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionType = 'EXPENSE' AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "GROUP BY t.category.id, t.category.name, t.category.colorCode, t.category.icon ORDER BY SUM(t.amount) DESC")
    List<Object[]> findDetailedCategoryExpenseBreakdown(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    long countByUserIdAndTransactionDateBetween(Long userId, Instant startDate, Instant endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionType = 'EXPENSE' AND t.transactionDate >= :startDate ORDER BY t.transactionDate ASC")
    List<Transaction> findExpensesByUserIdSince(@Param("userId") Long userId, @Param("startDate") Instant startDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT t.category.id, t.category.name, t.transactionType, SUM(t.amount), COUNT(t.id) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "GROUP BY t.category.id, t.category.name, t.transactionType ORDER BY SUM(t.amount) DESC")
    List<Object[]> findDetailedCategoryBreakdownAllTypes(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
