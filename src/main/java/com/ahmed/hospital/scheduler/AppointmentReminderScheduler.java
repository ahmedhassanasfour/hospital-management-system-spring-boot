package com.ahmed.hospital.scheduler;

import com.ahmed.hospital.appointment.service.AppointmentReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentReminderService appointmentReminderService;

    @Scheduled(cron = "0 0 * * * *")
    public void sendAppointmentReminders() {

        log.info("Running appointment reminder job");


        appointmentReminderService
                .processUpcomingAppointments();
        // Later:
        // 1. Find upcoming appointments
        // 2. Create notifications
        // 3. Send email
        // 4. Send WebSocket notification
    }
}