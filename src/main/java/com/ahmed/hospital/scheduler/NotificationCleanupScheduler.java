package com.ahmed.hospital.scheduler;

import com.ahmed.hospital.notification.service.NotificationCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers daily notification cleanup.
 * Scheduler decides WHEN; {@link NotificationCleanupService} decides WHAT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationCleanupService notificationCleanupService;

    /** Runs every day at 02:00 AM. */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldNotifications() {

        log.info("Running notification cleanup job");

        notificationCleanupService.deleteOldNotifications();
    }
}