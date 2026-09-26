package com.ahmed.hospital.notification.service;

import com.ahmed.hospital.notification.dto.NotificationResponse;
import com.ahmed.hospital.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse createNotification(
            Long userId,
            NotificationType type,
            String title,
            String message
    );

    Page<NotificationResponse> getUserNotifications(
            Long userId,
            Pageable pageable
    );

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAsUnread(Long notificationId, Long userId);
}