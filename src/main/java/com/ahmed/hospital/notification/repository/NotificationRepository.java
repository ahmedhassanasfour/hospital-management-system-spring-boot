package com.ahmed.hospital.notification.repository;

import com.ahmed.hospital.notification.entity.Notification;
import com.ahmed.hospital.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    /**
     * Used by the reminder scheduler to avoid creating duplicate reminder notifications.
     * There is no entityId field on Notification; we use the message to look for existing ones.
     * A simpler and accurate check: does a notification of the given type exist for this user
     * created after a certain time threshold?
     */
    boolean existsByUserIdAndTypeAndCreatedAtAfter(
            Long userId,
            NotificationType type,
            LocalDateTime createdAfter
    );

    /**
     * Hard-deletes notifications older than the given threshold.
     * Called by {@code NotificationCleanupScheduler}.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}