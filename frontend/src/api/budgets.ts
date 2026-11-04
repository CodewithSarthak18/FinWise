import { apiClient } from './client';
import { Budget } from '../types';

export const budgetsApi = {
  getCurrentBudget: async (): Promise<Budget> => {
    const response = await apiClient.get('/budgets/current');
    return response.data.data;
  },

  getBudgets: async (): Promise<Budget[]> => {
    const response = await apiClient.get('/budgets');
    return response.data.data;
  },

  getBudgetById: async (id: number): Promise<Budget> => {
    const response = await apiClient.get(`/budgets/${id}`);
    return response.data.data;
  },

  createBudget: async (data: {
    name: string;
    periodType: string;
    startDate: string;
    endDate: string;
    totalBudgetAmount: string;
    notificationThreshold?: number;
    categories: Array<{ categoryId: number; allocatedAmount: string }>;
  }): Promise<Budget> => {
    const response = await apiClient.post('/budgets', data);
    return response.data.data;
  },

  updateBudget: async (
    id: number,
    data: {
      name: string;
      periodType: string;
      startDate: string;
      endDate: string;
      totalBudgetAmount: string;
      notificationThreshold?: number;
      categories: Array<{ categoryId: number; allocatedAmount: string }>;
    }
  ): Promise<Budget> => {
    const response = await apiClient.put(`/budgets/${id}`, data);
    return response.data.data;
  },

  deleteBudget: async (id: number): Promise<void> => {
    await apiClient.delete(`/budgets/${id}`);
  },
};
