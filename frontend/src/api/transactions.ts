import { apiClient } from './client';
import { Category, PageResponse, Transaction } from '../types';

export const transactionsApi = {
  getCategories: async (type?: 'INCOME' | 'EXPENSE'): Promise<Category[]> => {
    const response = await apiClient.get('/categories', { params: { type } });
    const result = response.data?.data ?? response.data;
    return Array.isArray(result) ? result : [];
  },

  getTransactions: async (params?: {
    accountId?: number;
    categoryId?: number;
    transactionType?: string;
    startDate?: string;
    endDate?: string;
    search?: string;
    minAmount?: number;
    maxAmount?: number;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<PageResponse<Transaction>> => {
    const response = await apiClient.get('/transactions', { params });
    return response.data.data;
  },

  getTransactionById: async (id: number): Promise<Transaction> => {
    const response = await apiClient.get(`/transactions/${id}`);
    return response.data.data;
  },

  createTransaction: async (data: {
    accountId: number;
    categoryId: number;
    transactionType: string;
    amount: string;
    transactionDate: string;
    merchantName?: string;
    description?: string;
    notes?: string;
    paymentMode: string;
    referenceNumber?: string;
  }): Promise<Transaction> => {
    const response = await apiClient.post('/transactions', data);
    return response.data.data;
  },

  updateTransaction: async (
    id: number,
    data: {
      accountId: number;
      categoryId: number;
      transactionType: string;
      amount: string;
      transactionDate: string;
      merchantName?: string;
      description?: string;
      notes?: string;
      paymentMode: string;
      referenceNumber?: string;
    }
  ): Promise<Transaction> => {
    const response = await apiClient.put(`/transactions/${id}`, data);
    return response.data.data;
  },

  deleteTransaction: async (id: number): Promise<void> => {
    await apiClient.delete(`/transactions/${id}`);
  },

  refundTransaction: async (
    id: number,
    data?: { amount?: string; reason?: string }
  ): Promise<Transaction> => {
    const response = await apiClient.post(`/transactions/${id}/refund`, data || {});
    return response.data.data;
  },

  deleteTransfer: async (id: number): Promise<void> => {
    await apiClient.delete(`/transfers/${id}`);
  },
};
