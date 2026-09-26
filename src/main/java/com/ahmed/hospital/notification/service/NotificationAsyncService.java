package com.ahmed.hospital.notification.service;

import com.ahmed.hospital.email.service.EmailService;
import com.ahmed.hospital.email.template.EmailTemplateService;
import com.ahmed.hospital.notification.dto.NotificationResponse;
import com.ahmed.hospital.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Handles async side-effects after a notification has been persisted.
 * <p>
 * Must be a separate bean from {@link NotificationDispatcher} to avoid Spring
 * self-invocation, which would bypass the AOP proxy and lose the {@code @Async} behaviour.
 * <p>
 * All methods annotated with {@code @Async("taskExecutor")} run on the shared
 * {@code hospital-async-*} thread pool defined in {@code AsyncConfig}.
 * They never block the HTTP request thread or the notification-save transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAsyncService {

    private final NotificationWebSocketService webSocketService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    /**
     * Dispatches WebSocket push and, where appropriate, an email notification.
     * Runs on the {@code taskExecutor} thread pool.
     *
     * @param userId         notification recipient user ID
     * @param notification   already-persisted notification (safe to pass — not a JPA entity)
     * @param recipientEmail email address for the recipient
     * @param recipientName  display name for the recipient
     * @param type           notification type — determines email subject / template
     */
    @Async("taskExecutor")
    public void dispatchSideEffects(
            Long userId,
            NotificationResponse notification,
            String recipientEmail,
            String recipientName,
            NotificationType type
    ) {
        log.info(
                "Dispatching side-effects for notification {} (type={}) to userId={} on thread {}",
                notification.id(),
                type,
                userId,
                Thread.currentThread().getName()
        );

        // ── 1. WebSocket push ────────────────────────────────────────────────────
        try {
            webSocketService.sendNotification(userId, notification);
        } catch (Exception ex) {
            log.error(
                    "WebSocket delivery failed for notification {} userId={}: {}",
                    notification.id(),
                    userId,
                    ex.getMessage(),
                    ex
            );
        }

        // ── 2. Email (only for event types that warrant an email) ────────────────
        if (shouldSendEmail(type) && hasEmailAddress(recipientEmail)) {
            sendEmailForNotification(type, recipientEmail, recipientName, notification.message());
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────────

    private boolean shouldSendEmail(NotificationType type) {
        return switch (type) {
            case APPOINTMENT_CONFIRMED,
                 APPOINTMENT_CANCELLED,
                 APPOINTMENT_REMINDER,
                 PAYMENT_SUCCESS,
                 PAYMENT_FAILED,
                 LAB_RESULT_AVAILABLE,
                 PRESCRIPTION_CREATED -> true;
            // PASSWORD_RESET is handled by AuthService directly.
            default -> false;
        };
    }

    private boolean hasEmailAddress(String email) {
        return email != null && !email.isBlank();
    }

    private void sendEmailForNotification(
            NotificationType type,
            String recipientEmail,
            String recipientName,
            String message
    ) {
        try {
            String subject = buildSubject(type);

            // Use message directly as body for types without a dedicated template,
            // or use the template for appointment types that have rich templates.
            String body = buildEmailBody(type, recipientName, message);

            emailService.sendEmail(recipientEmail, subject, body);

        } catch (Exception ex) {
            // Email failure must NOT propagate — it must NOT undo the saved notification
            // or fail the async task visibly.
            log.error(
                    "Email dispatch failed for type={} to={}: {}",
                    type,
                    recipientEmail,
                    ex.getMessage(),
                    ex
            );
        }
    }

    private String buildSubject(NotificationType type) {
        return switch (type) {
            case APPOINTMENT_CONFIRMED -> "Appointment Confirmed — Hospital Management System";
            case APPOINTMENT_CANCELLED -> "Appointment Cancelled — Hospital Management System";
            case APPOINTMENT_REMINDER  -> "Appointment Reminder — Hospital Management System";
            case PAYMENT_SUCCESS       -> "Payment Received — Hospital Management System";
            case PAYMENT_FAILED        -> "Payment Failed — Hospital Management System";
            case LAB_RESULT_AVAILABLE  -> "Your Lab Results Are Ready — Hospital Management System";
            case PRESCRIPTION_CREATED  -> "New Prescription — Hospital Management System";
            default                    -> "Notification — Hospital Management System";
        };
    }

    private String buildEmailBody(
            NotificationType type,
            String recipientName,
            String fallbackMessage
    ) {
        // For notification types that have dedicated templates, use them.
        // For others, use the notification message directly with a personalised greeting.
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "User";

        return switch (type) {
            case APPOINTMENT_CONFIRMED, APPOINTMENT_CANCELLED, APPOINTMENT_REMINDER,
                 PAYMENT_SUCCESS, PAYMENT_FAILED, LAB_RESULT_AVAILABLE, PRESCRIPTION_CREATED ->
                    ("Hello %s,\n\n%s\n\nThank you,\nHospital Management System")
                            .formatted(name, fallbackMessage);
            default -> fallbackMessage;
        };
    }
}