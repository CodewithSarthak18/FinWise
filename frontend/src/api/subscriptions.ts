import { apiClient } from './client';
import { DetectedSubscription } from '../types';

export interface SubscriptionDetectionSummary {
  suggestions: DetectedSubscription[];
  totalCandidates: number;
  totalEstimatedMonthlyCost: string;
  totalEstimatedAnnualCost: string;
  notice: string;
}

export const subscriptionsApi = {
  getDetectedSubscriptions: async (
    months: number = 6
  ): Promise<SubscriptionDetectionSummary> => {
    const response = await apiClient.get('/subscriptions/detected', { params: { months } });
    const raw = response.data?.data ?? response.data ?? {};

    // Support both 'subscriptions' (backend DTO) and 'suggestions' (legacy/mock)
    const rawList: any[] = Array.isArray(raw.subscriptions)
      ? raw.subscriptions
      : Array.isArray(raw.suggestions)
      ? raw.suggestions
      : [];

    const suggestions: DetectedSubscription[] = rawList.map((item) => {
      const confidence = typeof item.confidenceScore === 'number'
        ? (item.confidenceScore <= 1 ? Math.round(item.confidenceScore * 100) : Math.round(item.confidenceScore))
        : 85;

      return {
        candidateId: item.candidateId || `sub-${Math.random().toString(36).substring(7)}`,
        subscriptionName: item.subscriptionName || item.name || 'Unknown Subscription',
        averagePayment: (item.averagePayment ?? '0.00').toString(),
        frequency: item.frequency || 'MONTHLY',
        frequencyLabel: item.frequencyLabel || 'Monthly',
        estimatedAnnualCost: (item.estimatedAnnualCost ?? '0.00').toString(),
        lastPaymentDate: item.lastPaymentDate || '',
        suggestedNextPaymentDate: item.suggestedNextPaymentDate || '',
        occurrences: item.occurrenceCount ?? item.occurrences ?? 0,
        occurrenceCount: item.occurrenceCount ?? item.occurrences ?? 0,
        confidenceScore: confidence,
        status: item.status || 'SUGGESTION',
        isSuggestion: item.isSuggestion ?? true,
        accountId: item.accountId,
        accountName: item.accountName,
        categoryId: item.categoryId,
        categoryName: item.categoryName,
        transactionDescriptions: item.transactionDescriptions || [],
      };
    });

    return {
      suggestions,
      totalCandidates: raw.detectedSubscriptionsCount ?? raw.totalCandidates ?? suggestions.length,
      totalEstimatedMonthlyCost: (raw.totalEstimatedMonthlyOutflow ?? '0.00').toString(),
      totalEstimatedAnnualCost: (
        raw.totalEstimatedAnnualOutflow ??
        raw.totalEstimatedAnnualCost ??
        '0.00'
      ).toString(),
      notice: raw.notice || 'All detected recurring expenses are marked as suggestions until confirmed by you.',
    };
  },

  confirmSubscription: async (data: {
    candidateId?: string;
    subscriptionName: string;
    amount: string;
    frequency: string;
    accountId: number;
    categoryId: number;
    startDate?: string;
    nextExecutionDate?: string;
    autoPost?: boolean;
  }): Promise<void> => {
    await apiClient.post('/subscriptions/confirm', {
      subscriptionName: data.subscriptionName,
      amount: data.amount,
      frequency: data.frequency,
      accountId: data.accountId,
      categoryId: data.categoryId,
      startDate: data.startDate || data.nextExecutionDate || new Date().toISOString().split('T')[0],
      autoPost: data.autoPost ?? true,
    });
  },

  dismissSubscription: async (candidateId: string): Promise<void> => {
    await apiClient.post(`/subscriptions/${candidateId}/dismiss`);
  },
};
