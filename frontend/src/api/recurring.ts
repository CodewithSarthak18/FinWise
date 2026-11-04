import { apiClient } from './client';
import { RecurringTransaction, UpcomingReminder } from '../types';

export const recurringApi = {
  getRecurringTransactions: async (): Promise<RecurringTransaction[]> => {
    const response = await apiClient.get('/recurring');
    return response.data.data;
  },

  getUpcomingReminders: async (days: number = 7): Promise<UpcomingReminder[]> => {
    const response = await apiClient.get('/recurring/upcoming', { params: { days } });
    return response.data.data;
  },

  createRecurring: async (data: {
    accountId: number;
    categoryId: number;
    title: string;
    amount: string;
    transactionType: string;
    frequency: string;
    startDate: string;
    endDate?: string;
    autoPost?: boolean;
    description?: string;
  }): Promise<RecurringTransaction> => {
    const response = await apiClient.post('/recurring', data);
    return response.data.data;
  },

  pauseRecurring: async (id: number): Promise<RecurringTransaction> => {
    const response = await apiClient.patch(`/recurring/${id}/pause`);
    return response.data.data;
  },

  resumeRecurring: async (id: number): Promise<RecurringTransaction> => {
    const response = await apiClient.patch(`/recurring/${id}/resume`);
    return response.data.data;
  },

  executeRecurring: async (id: number): Promise<RecurringTransaction> => {
    const response = await apiClient.post(`/recurring/${id}/execute`);
    return response.data.data;
  },

  deleteRecurring: async (id: number): Promise<void> => {
    await apiClient.delete(`/recurring/${id}`);
  },
};
