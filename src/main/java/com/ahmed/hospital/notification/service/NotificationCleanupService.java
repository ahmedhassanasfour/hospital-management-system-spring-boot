package com.ahmed.hospital.notification.service;

import com.ahmed.hospital.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles cleanup of old, read notifications to prevent unbounded table growth.
 * Called by {@link com.ahmed.hospital.scheduler.NotificationCleanupScheduler}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    /**
     * Number of days to retain notifications.
     * Configurable via {@code notification.cleanup.retention-days} (default: 90).
     */
    @Value("${notification.cleanup.retention-days:90}")
    private int retentionDays;

    @Transactional
    public void deleteOldNotifications() {

        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);

        int deleted = notificationRepository.deleteByCreatedAtBefore(threshold);

        log.info(
                "Notification cleanup: deleted {} notification(s) older than {} days (threshold: {})",
                deleted,
                retentionDays,
                threshold
        );
    }
}
