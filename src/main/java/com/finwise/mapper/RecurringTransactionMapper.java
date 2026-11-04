package com.finwise.mapper;

import com.finwise.dto.recurring.RecurringTransactionRequest;
import com.finwise.dto.recurring.RecurringTransactionResponse;
import com.finwise.dto.recurring.UpcomingReminderResponse;
import com.finwise.entity.Account;
import com.finwise.entity.Category;
import com.finwise.entity.RecurringTransaction;
import com.finwise.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class RecurringTransactionMapper {

    public RecurringTransaction toEntity(RecurringTransactionRequest request, User user, Account account, Category category) {
        if (request == null) {
            return null;
        }

        LocalDate nextExecution = request.getNextExecutionDate() != null 
                ? request.getNextExecutionDate() 
                : request.getStartDate();

        return RecurringTransaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .title(request.getTitle())
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextExecutionDate(nextExecution)
                .autoPost(request.getAutoPost() != null ? request.getAutoPost() : true)
                .active(true)
                .description(request.getDescription())
                .build();
    }

    public RecurringTransactionResponse toResponse(RecurringTransaction entity) {
        if (entity == null) {
            return null;
        }

        return RecurringTransactionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .accountId(entity.getAccount() != null ? entity.getAccount().getId() : null)
                .accountName(entity.getAccount() != null ? entity.getAccount().getAccountName() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .categoryColor(entity.getCategory() != null ? entity.getCategory().getColorCode() : null)
                .categoryIcon(entity.getCategory() != null ? entity.getCategory().getIcon() : null)
                .title(entity.getTitle())
                .amount(entity.getAmount())
                .transactionType(entity.getTransactionType())
                .frequency(entity.getFrequency())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .nextExecutionDate(entity.getNextExecutionDate())
                .autoPost(entity.isAutoPost())
                .active(entity.isActive())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UpcomingReminderResponse toReminderResponse(RecurringTransaction entity) {
        if (entity == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, entity.getNextExecutionDate());

        return UpcomingReminderResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .amount(entity.getAmount())
                .transactionType(entity.getTransactionType())
                .frequency(entity.getFrequency())
                .dueDate(entity.getNextExecutionDate())
                .daysUntilDue(daysUntilDue)
                .accountId(entity.getAccount() != null ? entity.getAccount().getId() : null)
                .accountName(entity.getAccount() != null ? entity.getAccount().getAccountName() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .autoPost(entity.isAutoPost())
                .active(entity.isActive())
                .description(entity.getDescription())
                .build();
    }
}
