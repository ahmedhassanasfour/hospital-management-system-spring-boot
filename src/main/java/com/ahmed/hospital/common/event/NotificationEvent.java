package com.ahmed.hospital.common.event;

import com.ahmed.hospital.notification.entity.NotificationType;

/**
 * A generic domain event carrying the minimum data needed to create a notification.
 * <p>
 * Only primitive values and IDs are carried — no lazy JPA entities — so this is safe
 * to pass through Spring's {@code ApplicationEventPublisher} and across a transaction
 * boundary into an {@code @TransactionalEventListener(AFTER_COMMIT)} handler.
 *
 * @param userId           ID of the {@code User} who should receive the notification
 * @param type             the notification type
 * @param title            short title shown in the UI
 * @param message          full message body
 * @param recipientEmail   email address of the recipient (for async email dispatch)
 * @param recipientName    display name of the recipient (used in email templates)
 * @param entityId         ID of the related business entity (appointment, payment, etc.)
 */
public record NotificationEvent(
        Long userId,
        NotificationType type,
        String title,
        String message,
        String recipientEmail,
        String recipientName,
        Long entityId
) {
}
