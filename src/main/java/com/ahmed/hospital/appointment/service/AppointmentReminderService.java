package com.ahmed.hospital.appointment.service;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.appointment.repository.AppointmentRepository;
import com.ahmed.hospital.notification.entity.NotificationType;
import com.ahmed.hospital.notification.repository.NotificationRepository;
import com.ahmed.hospital.notification.service.NotificationService;
import com.ahmed.hospital.notification.service.NotificationAsyncService;
import com.ahmed.hospital.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements the reminder logic called by {@link com.ahmed.hospital.scheduler.AppointmentReminderScheduler}.
 * <p>
 * Responsibility split:
 * <ul>
 *   <li>Scheduler — <strong>WHEN</strong> (cron trigger)</li>
 *   <li>This service — <strong>WHAT</strong> (find appointments, deduplicate, create notifications)</li>
 * </ul>
 * <p>
 * The scheduler is NOT event-driven because it is initiated by a timer, not by a business operation.
 * Instead this service creates notifications directly in its own transaction and dispatches async
 * side-effects explicitly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationAsyncService notificationAsyncService;

    /**
     * Called every hour by the scheduler.
     * Finds CONFIRMED appointments in the next 24 hours and sends a reminder
     * notification to each patient — but only if one was not already sent today.
     */
    @Transactional
    public void processUpcomingAppointments() {

        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Appointment> upcoming =
                appointmentRepository
                        .findUpcomingAppointmentsForReminder(today, tomorrow);

        log.info(
                "Appointment reminder job: {} upcoming CONFIRMED appointment(s) found",
                upcoming.size()
        );

        for (Appointment appointment : upcoming) {

            Long patientUserId = appointment.getPatient().getUser().getId();

            // Deduplication: skip if a reminder was already sent in the last 20 hours.
            // This prevents duplicate reminders when the cron fires multiple times in a day.
            boolean alreadySent = notificationRepository
                    .existsByUserIdAndTypeAndCreatedAtAfter(
                            patientUserId,
                            NotificationType.APPOINTMENT_REMINDER,
                            LocalDateTime.now().minusHours(20)
                    );

            if (alreadySent) {
                log.debug(
                        "Skipping reminder for appointment {} — already sent recently",
                        appointment.getId()
                );
                continue;
            }

            String patientName = appointment.getPatient().getUser().getFirstName()
                    + " " + appointment.getPatient().getUser().getLastName();
            String patientEmail = appointment.getPatient().getUser().getEmail();

            String doctorName = appointment.getDoctor().getUser().getFirstName()
                    + " " + appointment.getDoctor().getUser().getLastName();

            String message = "Reminder: you have an appointment with Dr. "
                    + doctorName + " on " + appointment.getDate()
                    + " at " + appointment.getStartTime() + ".";

            try {
                // Save the notification directly (this IS the transaction — no event needed)
                NotificationResponse saved = notificationService.createNotification(
                        patientUserId,
                        NotificationType.APPOINTMENT_REMINDER,
                        "Appointment Reminder",
                        message
                );

                log.info(
                        "Reminder notification {} created for patientUserId={}",
                        saved.id(),
                        patientUserId
                );

                // Dispatch WebSocket + email asynchronously
                notificationAsyncService.dispatchSideEffects(
                        patientUserId,
                        saved,
                        patientEmail,
                        patientName,
                        NotificationType.APPOINTMENT_REMINDER
                );

            } catch (Exception ex) {
                log.error(
                        "Failed to send reminder for appointment {}: {}",
                        appointment.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }
}