package com.finwise.dto.recurring;

import com.finwise.entity.RecurringFrequency;
import com.finwise.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingReminderResponse {

    private Long id;
    private String title;
    private BigDecimal amount;
    private TransactionType transactionType;
    private RecurringFrequency frequency;
    private LocalDate dueDate;
    private long daysUntilDue;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private boolean autoPost;
    private boolean active;
    private String description;
}
