package com.finwise.service;

import com.finwise.dto.recurring.RecurringTransactionRequest;
import com.finwise.dto.recurring.RecurringTransactionResponse;
import com.finwise.dto.recurring.UpcomingReminderResponse;
import com.finwise.entity.RecurringFrequency;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionService {

    RecurringTransactionResponse createRecurringTransaction(RecurringTransactionRequest request, Long userId);

    List<RecurringTransactionResponse> getRecurringTransactions(Long userId);

    RecurringTransactionResponse getRecurringTransactionById(Long id, Long userId);

    RecurringTransactionResponse updateRecurringTransaction(Long id, RecurringTransactionRequest request, Long userId);

    void deleteRecurringTransaction(Long id, Long userId);

    RecurringTransactionResponse pauseRecurringTransaction(Long id, Long userId);

    RecurringTransactionResponse resumeRecurringTransaction(Long id, Long userId);

    List<UpcomingReminderResponse> getUpcomingRecurring(Long userId, int daysAhead);

    LocalDate calculateNextExecutionDate(LocalDate currentDate, RecurringFrequency frequency);

    int processDueRecurringTransactions();

    RecurringTransactionResponse executeRecurringTransaction(Long id, Long userId);
}
