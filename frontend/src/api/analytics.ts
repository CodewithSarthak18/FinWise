import { apiClient } from './client';
import {
  CategoryExpense,
  DashboardSummary,
  FinancialHealthScore,
  IncomeVsExpense,
  MonthlySpendingTrend,
  NeedsWantsBreakdown,
  SmartInsight,
} from '../types';

export const analyticsApi = {
  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const response = await apiClient.get('/analytics/dashboard-summary');
    return response.data.data;
  },

  getTotalIncome: async (month?: string): Promise<{ totalIncome: string; period: string }> => {
    const response = await apiClient.get('/analytics/total-income', { params: { month } });
    return response.data.data;
  },

  getTotalExpenses: async (month?: string): Promise<{ totalExpenses: string; period: string }> => {
    const response = await apiClient.get('/analytics/total-expenses', { params: { month } });
    return response.data.data;
  },

  getNetSavings: async (month?: string): Promise<{ netSavings: string; period: string }> => {
    const response = await apiClient.get('/analytics/net-savings', { params: { month } });
    return response.data.data;
  },

  getSavingsRate: async (month?: string): Promise<{ savingsRate: number; period: string }> => {
    const response = await apiClient.get('/analytics/savings-rate', { params: { month } });
    return response.data.data;
  },

  getSpendingTrends: async (months: number = 6): Promise<MonthlySpendingTrend[]> => {
    const response = await apiClient.get('/analytics/spending-trends', { params: { months } });
    return response.data.data;
  },

  getCategoryExpenses: async (month?: string): Promise<CategoryExpense[]> => {
    const response = await apiClient.get('/analytics/category-expenses', { params: { month } });
    return response.data.data;
  },

  getIncomeVsExpense: async (months: number = 6): Promise<IncomeVsExpense[]> => {
    const response = await apiClient.get('/analytics/income-vs-expense', { params: { months } });
    return response.data.data;
  },

  getNeedsVsWants: async (month?: string): Promise<NeedsWantsBreakdown> => {
    const response = await apiClient.get('/analytics/needs-vs-wants', { params: { month } });
    return response.data.data;
  },

  getFinancialHealthScore: async (month?: string): Promise<FinancialHealthScore> => {
    const response = await apiClient.get('/analytics/financial-health-score', { params: { month } });
    return response.data.data;
  },

  getSmartInsights: async (month?: string): Promise<{ insights: SmartInsight[] }> => {
    const response = await apiClient.get('/analytics/smart-insights', { params: { month } });
    return response.data.data;
  },
};
