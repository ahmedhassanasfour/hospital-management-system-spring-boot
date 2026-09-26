package com.ahmed.hospital.notification.service;

import com.ahmed.hospital.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(
            Long userId,
            NotificationResponse notification
    ) {

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                notification
        );
    }
}