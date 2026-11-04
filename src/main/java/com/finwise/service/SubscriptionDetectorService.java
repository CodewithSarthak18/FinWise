package com.finwise.service;

import com.finwise.dto.recurring.RecurringTransactionResponse;
import com.finwise.dto.subscription.ConfirmSubscriptionRequest;
import com.finwise.dto.subscription.DetectedSubscriptionResponse;
import com.finwise.dto.subscription.SubscriptionDetectionSummaryResponse;

import java.util.List;

public interface SubscriptionDetectorService {

    SubscriptionDetectionSummaryResponse detectSubscriptions(Long userId, int lookbackMonths);

    List<DetectedSubscriptionResponse> getDetectedSubscriptions(Long userId);

    RecurringTransactionResponse confirmDetectedSubscription(Long userId, ConfirmSubscriptionRequest request);

    void dismissDetectedSubscription(Long userId, String candidateId);
}
