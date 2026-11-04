package com.finwise.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDetectionSummaryResponse {

    private int detectedSubscriptionsCount;
    private BigDecimal totalEstimatedMonthlyOutflow;
    private BigDecimal totalEstimatedAnnualOutflow;
    private String notice; // "All detected recurring expenses are marked as suggestions until confirmed by you."
    private List<DetectedSubscriptionResponse> subscriptions;
}
