package com.finwise.repository.specification;

import com.finwise.entity.Transaction;
import com.finwise.entity.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<Transaction> withFilters(
            Long userId,
            Long accountId,
            Long categoryId,
            TransactionType transactionType,
            Instant startDate,
            Instant endDate,
            String search,
            BigDecimal minAmount,
            BigDecimal maxAmount) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Mandatory tenant isolation
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (accountId != null) {
                predicates.add(cb.equal(root.get("account").get("id"), accountId));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }

            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate merchantMatch = cb.like(cb.lower(root.get("merchantName")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate refMatch = cb.like(cb.lower(root.get("referenceNumber")), searchPattern);
                Predicate notesMatch = cb.like(cb.lower(root.get("notes")), searchPattern);
                predicates.add(cb.or(merchantMatch, descMatch, refMatch, notesMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
