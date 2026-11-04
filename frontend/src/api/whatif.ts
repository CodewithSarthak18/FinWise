import { apiClient } from './client';
import { WhatIfSimulationResult } from '../types';

export const whatIfApi = {
  getSpendingCategories: async (
    month?: string
  ): Promise<Array<{ categoryId: number; categoryName: string; averageMonthlySpending: string }>> => {
    const response = await apiClient.get('/calculator/what-if/categories', { params: { month } });
    return response.data.data;
  },

  simulate: async (categoryId: number, reductionPercentage: number): Promise<WhatIfSimulationResult> => {
    const response = await apiClient.post('/calculator/what-if', { categoryId, reductionPercentage });
    return response.data.data;
  },
};
