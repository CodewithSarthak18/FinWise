import { apiClient } from './client';
import { Account } from '../types';

export const accountsApi = {
  getAccounts: async (): Promise<Account[]> => {
    const response = await apiClient.get('/accounts');
    const result = response.data?.data ?? response.data;
    return Array.isArray(result) ? result : [];
  },

  getAccountById: async (id: number): Promise<Account> => {
    const response = await apiClient.get(`/accounts/${id}`);
    return response.data.data;
  },

  createAccount: async (data: {
    accountName: string;
    accountType: string;
    institutionName?: string;
    accountNumberMask?: string;
    initialBalance: string;
    currencyCode?: string;
    creditLimit?: string;
  }): Promise<Account> => {
    const response = await apiClient.post('/accounts', data);
    return response.data.data;
  },

  updateAccount: async (
    id: number,
    data: {
      accountName: string;
      accountType: string;
      institutionName?: string;
      creditLimit?: string;
    }
  ): Promise<Account> => {
    const response = await apiClient.put(`/accounts/${id}`, data);
    return response.data.data;
  },

  deleteAccount: async (id: number): Promise<void> => {
    await apiClient.delete(`/accounts/${id}`);
  },
};
