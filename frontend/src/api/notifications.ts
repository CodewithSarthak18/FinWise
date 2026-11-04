import { apiClient } from './client';
import { InAppNotification } from '../types';

export const notificationsApi = {
  getNotifications: async (limit: number = 20): Promise<InAppNotification[]> => {
    const response = await apiClient.get('/notifications', { params: { limit } });
    return response.data.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await apiClient.get('/notifications/unread-count');
    return response.data.data.count;
  },

  markAsRead: async (id: number): Promise<void> => {
    await apiClient.patch(`/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.patch('/notifications/read-all');
  },
};
