package com.finwise.repository;

import com.finwise.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Page<Transfer> findByUserId(Long userId, Pageable pageable);

    Optional<Transfer> findByIdAndUserId(Long id, Long userId);
}
