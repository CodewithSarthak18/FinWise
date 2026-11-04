package com.finwise.dto.recurring;

import com.finwise.entity.RecurringFrequency;
import com.finwise.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionResponse {

    private Long id;
    private Long userId;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private String title;
    private BigDecimal amount;
    private TransactionType transactionType;
    private RecurringFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExecutionDate;
    private boolean autoPost;
    private boolean active;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
