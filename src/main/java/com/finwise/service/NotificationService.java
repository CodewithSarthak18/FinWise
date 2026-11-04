package com.finwise.service;

import com.finwise.dto.notification.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getNotificationsForUser(Long userId, int limit);

    long getUnreadCountForUser(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
