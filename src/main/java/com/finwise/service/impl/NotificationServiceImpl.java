package com.finwise.service.impl;

import com.finwise.dto.notification.NotificationResponse;
import com.finwise.entity.Notification;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.repository.NotificationRepository;
import com.finwise.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationResponse> getNotificationsForUser(Long userId, int limit) {
        log.debug("Fetching up to {} notifications for user id: {}", limit, userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .map(NotificationResponse::fromEntity)
                .getContent();
    }

    @Override
    public long getUnreadCountForUser(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        
        notification.setRead(true);
        notificationRepository.save(notification);
        log.debug("Marked notification {} as read for user {}", notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
        log.debug("Marked all notifications as read for user {}", userId);
    }
}
