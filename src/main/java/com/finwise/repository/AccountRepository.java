package com.finwise.repository;

import com.finwise.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Account> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(a.currentBalance), 0) FROM Account a WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    BigDecimal calculateTotalNetWorth(@Param("userId") Long userId);
}
