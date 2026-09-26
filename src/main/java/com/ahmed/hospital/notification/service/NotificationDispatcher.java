package com.ahmed.hospital.notification.service;

import com.ahmed.hospital.common.event.NotificationEvent;
import com.ahmed.hospital.notification.dto.NotificationResponse;
import com.ahmed.hospital.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Central dispatcher for notification side-effects.
 * <p>
 * Listens for {@link NotificationEvent} after the publishing transaction commits
 * ({@code AFTER_COMMIT}).  This guarantees that:
 * <ol>
 *   <li>Notifications are only persisted when the triggering business operation succeeded.</li>
 *   <li>WebSocket and email side-effects run asynchronously, never blocking the HTTP response.</li>
 * </ol>
 * <p>
 * Runs in a NEW transaction so the notification save is independent of the original
 * (already committed) business transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final NotificationService notificationService;
    private final NotificationAsyncService notificationAsyncService;

    /**
     * Called automatically after the triggering transaction commits.
     * Saves the notification in a new transaction, then kicks off async side-effects.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationEvent event) {

        log.debug(
                "Handling notification event: type={}, userId={}",
                event.type(),
                event.userId()
        );

        try {
            NotificationResponse saved = notificationService.createNotification(
                    event.userId(),
                    event.type(),
                    event.title(),
                    event.message()
            );

            // Async: push to WebSocket + send email.
            // These run on the taskExecutor thread pool and do NOT block.
            notificationAsyncService.dispatchSideEffects(
                    event.userId(),
                    saved,
                    event.recipientEmail(),
                    event.recipientName(),
                    event.type()
            );

        } catch (Exception ex) {
            // Side-effect failure must NOT bubble up and affect the already-committed
            // business transaction.  Log the failure and continue.
            log.error(
                    "Failed to handle notification event: type={}, userId={} — {}",
                    event.type(),
                    event.userId(),
                    ex.getMessage(),
                    ex
            );
        }
    }
}
