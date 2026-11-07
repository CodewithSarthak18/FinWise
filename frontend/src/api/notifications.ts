import { apiClient } from './client';
import { InAppNotification } from '../types';

export const notificationsApi = {
  getNotifications: async (limit: number = 20): Promise<InAppNotification[]> => {
    try {
      const response = await apiClient.get('/notifications', { params: { limit } });
      const data = response.data?.data ?? response.data;
      return Array.isArray(data) ? data : [];
    } catch {
      return [];
    }
  },

  getUnreadCount: async (): Promise<number> => {
    try {
      const response = await apiClient.get('/notifications/unread-count');
      const data = response.data?.data ?? response.data;
      return typeof data?.count === 'number' ? data.count : 0;
    } catch {
      return 0;
    }
  },

  markAsRead: async (id: number): Promise<void> => {
    await apiClient.patch(`/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.patch('/notifications/read-all');
  },
};
