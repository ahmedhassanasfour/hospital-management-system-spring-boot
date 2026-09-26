package com.ahmed.hospital.notification.service;

import com.ahmed.hospital.notification.dto.NotificationResponse;
import com.ahmed.hospital.notification.entity.Notification;
import com.ahmed.hospital.notification.entity.NotificationType;
import com.ahmed.hospital.notification.repository.NotificationRepository;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.repository.UserRepository;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationResponse createNotification(
            Long userId,
            NotificationType type,
            String title,
            String message
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .build();

        return mapToResponse(
                notificationRepository.save(notification)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(
            Long userId,
            Pageable pageable
    ) {
        return notificationRepository
                .findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }


    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository
                .countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {

        Notification notification = getUserNotification(
                notificationId,
                userId
        );

        notification.setRead(true);
    }

    @Override
    public void markAsUnread(Long notificationId, Long userId) {

        Notification notification = getUserNotification(
                notificationId,
                userId
        );

        notification.setRead(false);
    }

    private Notification getUserNotification(
            Long notificationId,
            Long userId
    ) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Notification not found"
                        ));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(
                    "Notification not found"
            );
        }

        return notification;
    }


    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}