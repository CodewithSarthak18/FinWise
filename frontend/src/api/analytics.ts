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
    const d = response.data?.data || response.data || {};
    const netWorth = (d.netWorth ?? '0.00').toString();
    const income = (d.monthlyIncome ?? d.currentMonthIncome ?? '0.00').toString();
    const expense = (d.monthlyExpense ?? d.currentMonthExpense ?? '0.00').toString();
    const savingsRate = Number(d.savingsRate ?? d.savingsRatePercentage ?? 0);
    const budgetUtil = Number(d.budgetUtilization ?? d.budgetUtilizationPercentage ?? 0);
    return {
      netWorth,
      formattedNetWorth: `₹${netWorth}`,
      currentMonthIncome: income,
      formattedCurrentMonthIncome: `₹${income}`,
      currentMonthExpense: expense,
      formattedCurrentMonthExpense: `₹${expense}`,
      savingsRatePercentage: savingsRate,
      activeAccountsCount: d.activeAccountsCount ?? 0,
      activeBudgetsCount: d.activeBudgetsCount ?? (d.hasActiveBudget ? 1 : 0),
      budgetUtilizationPercentage: budgetUtil,
    };
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
    const raw = response.data?.data || response.data || {};

    const components: Array<{
      componentName: string;
      score: number;
      maxScore: number;
      feedback: string;
    }> = [];

    if (Array.isArray(raw.components) && raw.components.length > 0) {
      components.push(...raw.components);
    } else {
      if (raw.savingsRateComponent) {
        components.push({
          componentName: raw.savingsRateComponent.name || 'Savings Rate',
          score: Number(raw.savingsRateComponent.earnedScore ?? 0),
          maxScore: Number(raw.savingsRateComponent.maxScore ?? 40),
          feedback: raw.savingsRateComponent.recommendation || raw.savingsRateComponent.evaluation || '',
        });
      }
      if (raw.budgetAdherenceComponent) {
        components.push({
          componentName: raw.budgetAdherenceComponent.name || 'Budget Adherence',
          score: Number(raw.budgetAdherenceComponent.earnedScore ?? 0),
          maxScore: Number(raw.budgetAdherenceComponent.maxScore ?? 35),
          feedback: raw.budgetAdherenceComponent.recommendation || raw.budgetAdherenceComponent.evaluation || '',
        });
      }
      if (raw.spendingConsistencyComponent) {
        components.push({
          componentName: raw.spendingConsistencyComponent.name || 'Spending Consistency',
          score: Number(raw.spendingConsistencyComponent.earnedScore ?? 0),
          maxScore: Number(raw.spendingConsistencyComponent.maxScore ?? 25),
          feedback: raw.spendingConsistencyComponent.recommendation || raw.spendingConsistencyComponent.evaluation || '',
        });
      }
    }

    return {
      score: Number(raw.score ?? raw.overallScore ?? 0),
      ratingGrade: raw.ratingGrade ?? raw.ratingBand ?? 'GOOD',
      evaluationSummary: raw.evaluationSummary ?? raw.summary ?? '',
      components,
      calculationDate: raw.calculationDate || new Date().toISOString(),
    };
  },

  getSmartInsights: async (month?: string): Promise<{ insights: SmartInsight[] }> => {
    const response = await apiClient.get('/analytics/smart-insights', { params: { month } });
    return response.data.data;
  },
};
