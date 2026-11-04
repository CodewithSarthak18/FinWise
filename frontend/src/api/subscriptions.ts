import { apiClient } from './client';
import { DetectedSubscription } from '../types';

export const subscriptionsApi = {
  getDetectedSubscriptions: async (
    months: number = 6
  ): Promise<{ suggestions: DetectedSubscription[]; totalCandidates: number; totalEstimatedAnnualCost: string }> => {
    const response = await apiClient.get('/subscriptions/detected', { params: { months } });
    return response.data.data;
  },

  confirmSubscription: async (data: {
    candidateId: string;
    subscriptionName: string;
    amount: string;
    frequency: string;
    accountId: number;
    categoryId: number;
    nextExecutionDate: string;
  }): Promise<void> => {
    await apiClient.post('/subscriptions/confirm', data);
  },

  dismissSubscription: async (candidateId: string): Promise<void> => {
    await apiClient.post(`/subscriptions/${candidateId}/dismiss`);
  },
};
