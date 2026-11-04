package com.finwise.dto.subscription;

import com.finwise.entity.RecurringFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedSubscriptionResponse {

    private String candidateId;
    private String subscriptionName;
    private BigDecimal averagePayment;
    private RecurringFrequency frequency;
    private String frequencyLabel;
    private BigDecimal estimatedAnnualCost;
    private LocalDate lastPaymentDate;
    private LocalDate suggestedNextPaymentDate;
    private String status; // "SUGGESTION", "CONFIRMED"
    private boolean isSuggestion;
    private double confidenceScore;
    private int occurrenceCount;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private List<String> transactionDescriptions;
}
