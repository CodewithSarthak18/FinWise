import { apiClient } from './client';
import { Budget } from '../types';

const normalizeBudget = (raw: any): Budget => {
  if (!raw) return raw;
  return {
    ...raw,
    totalBudgetAmount: (raw.totalBudgetAmount ?? '0.00').toString(),
    totalSpent: (raw.totalSpent ?? raw.totalSpentAmount ?? '0.00').toString(),
    remainingBudget: (raw.remainingBudget ?? raw.remainingAmount ?? '0.00').toString(),
    overallAlertStatus: raw.overallAlertStatus ?? raw.alertStatus ?? 'NORMAL',
    utilizationPercentage: Number(raw.utilizationPercentage ?? 0),
    categories: (raw.categories || []).map((cat: any) => ({
      ...cat,
      spentAmount: (cat.spentAmount ?? '0.00').toString(),
      allocatedAmount: (cat.allocatedAmount ?? '0.00').toString(),
      remainingAmount: (cat.remainingAmount ?? '0.00').toString(),
      utilizationPercentage: Number(cat.utilizationPercentage ?? 0),
      alertStatus: cat.alertStatus ?? 'NORMAL',
    })),
  };
};

export const budgetsApi = {
  getCurrentBudget: async (): Promise<Budget> => {
    const response = await apiClient.get('/budgets/current');
    const data = response.data?.data || response.data;
    return normalizeBudget(data);
  },

  getBudgets: async (): Promise<Budget[]> => {
    const response = await apiClient.get('/budgets');
    const data = response.data?.data || response.data || [];
    return Array.isArray(data) ? data.map(normalizeBudget) : [];
  },

  getBudgetById: async (id: number): Promise<Budget> => {
    const response = await apiClient.get(`/budgets/${id}`);
    const data = response.data?.data || response.data;
    return normalizeBudget(data);
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
