import { apiClient } from './client';
import { GoalContribution, SavingsGoal } from '../types';

export const goalsApi = {
  getGoals: async (): Promise<SavingsGoal[]> => {
    try {
      const response = await apiClient.get('/goals');
      const data = response.data?.data ?? response.data;
      return Array.isArray(data) ? data : [];
    } catch {
      return [];
    }
  },

  getGoalById: async (id: number): Promise<SavingsGoal> => {
    const response = await apiClient.get(`/goals/${id}`);
    return response.data.data;
  },

  createGoal: async (data: {
    title: string;
    description?: string;
    targetAmount: string;
    targetDate: string;
    priority?: string;
  }): Promise<SavingsGoal> => {
    const response = await apiClient.post('/goals', data);
    return response.data.data;
  },

  contributeToGoal: async (
    id: number,
    data: {
      accountId: number;
      amount: string;
      note?: string;
    }
  ): Promise<SavingsGoal> => {
    const response = await apiClient.post(`/goals/${id}/contribute`, data);
    return response.data.data;
  },

  getGoalContributions: async (id: number): Promise<GoalContribution[]> => {
    try {
      const response = await apiClient.get(`/goals/${id}/contributions`);
      const data = response.data?.data ?? response.data;
      return Array.isArray(data) ? data : [];
    } catch {
      return [];
    }
  },

  deleteGoal: async (id: number): Promise<void> => {
    await apiClient.delete(`/goals/${id}`);
  },
};
