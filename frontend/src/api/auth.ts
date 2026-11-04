import { apiClient } from './client';
import { AuthResponseData, User } from '../types';

export const authApi = {
  login: async (credentials: { email: string; password: string }): Promise<AuthResponseData> => {
    const response = await apiClient.post('/auth/login', credentials);
    return response.data.data;
  },

  register: async (data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phoneNumber?: string;
    currencyCode?: string;
  }): Promise<AuthResponseData> => {
    const response = await apiClient.post('/auth/register', data);
    return response.data.data;
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get('/users/me');
    return response.data.data;
  },

  changePassword: async (data: { oldPassword: string; newPassword: string }): Promise<void> => {
    await apiClient.put('/users/me/password', data);
  },

  logout: async (): Promise<void> => {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      localStorage.removeItem('finwise_access_token');
      localStorage.removeItem('finwise_refresh_token');
      localStorage.removeItem('finwise_user');
    }
  },
};
