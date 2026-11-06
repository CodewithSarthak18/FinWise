import { apiClient } from './client';
import { AuthResponseData, User } from '../types';

export const authApi = {
  login: async (credentials: { email: string; password: string }): Promise<AuthResponseData> => {
    const response = await apiClient.post('/auth/login', credentials);
    const data = response.data?.data || response.data;
    const user: User = data.user || {
      id: data.userId || data.id,
      email: data.email,
      firstName: data.firstName,
      lastName: data.lastName,
      currencyCode: data.currencyCode || 'INR',
      roles: data.roles ? Array.from(data.roles) : ['ROLE_USER'],
    };
    return {
      tokenType: data.tokenType || 'Bearer',
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      expiresInSeconds: data.expiresIn || data.expiresInSeconds || 900,
      user,
    };
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
    const resData = response.data?.data || response.data;
    const user: User = resData.user || {
      id: resData.userId || resData.id,
      email: resData.email,
      firstName: resData.firstName,
      lastName: resData.lastName,
      phoneNumber: resData.phoneNumber,
      currencyCode: resData.currencyCode || 'INR',
      roles: resData.roles ? Array.from(resData.roles) : ['ROLE_USER'],
    };
    return {
      tokenType: resData.tokenType || 'Bearer',
      accessToken: resData.accessToken,
      refreshToken: resData.refreshToken,
      expiresInSeconds: resData.expiresIn || resData.expiresInSeconds || 900,
      user,
    };
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
