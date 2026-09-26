package com.ahmed.hospital.appointment.service;

import com.ahmed.hospital.appointment.dto.AppointmentResponse;
import com.ahmed.hospital.appointment.dto.CreateAppointmentRequest;
import com.ahmed.hospital.appointment.dto.UpdateAppointmentStatusRequest;
import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.appointment.entity.AppointmentStatus;
import com.ahmed.hospital.appointment.repository.AppointmentRepository;
import com.ahmed.hospital.audit.service.AuditService;
import com.ahmed.hospital.common.event.NotificationEvent;
import com.ahmed.hospital.common.exception.DuplicateResourceException;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.entity.DoctorBranch;
import com.ahmed.hospital.doctor.entity.DoctorSchedule;
import com.ahmed.hospital.doctor.repository.DoctorBranchRepository;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.doctor.repository.DoctorScheduleRepository;
import com.ahmed.hospital.notification.entity.NotificationType;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    @Transactional
    public AppointmentResponse createAppointment(
            Long doctorId,
            Long patientId,
            Long branchId,
            CreateAppointmentRequest request
    ) {

        Doctor doctor = findDoctorForUpdate(doctorId);

        Patient patient = findPatient(patientId);

        DoctorBranch doctorBranch =
                findActiveDoctorBranch(
                        doctorId,
                        branchId
                );

        validateDateTime(
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        validateDoctorSchedule(
                doctorBranch.getId(),
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        validateNoDoubleBooking(
                doctorId,
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .doctorBranch(doctorBranch)
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(AppointmentStatus.PENDING)
                .notes(request.getNotes())
                .build();

        Appointment saved =
                appointmentRepository.save(appointment);

        // Audit: appointment created
        auditService.log(
                patient.getUser().getId(),
                "APPOINTMENT_CREATED",
                "Appointment",
                saved.getId(),
                "Appointment created with doctor " + doctorId + " at branch " + branchId,
                null
        );

        return mapToResponse(saved);
    }

    public AppointmentResponse getAppointmentById(
            Long id
    ) {

        Appointment appointment =
                findAppointment(id);

        return mapToResponse(appointment);
    }

    public List<AppointmentResponse> getDoctorAppointments(
            Long doctorId
    ) {

        findDoctor(doctorId);

        return appointmentRepository
                .findByDoctorIdOrderByDateDescStartTimeDesc(
                        doctorId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AppointmentResponse> getPatientAppointments(
            Long patientId
    ) {

        findPatient(patientId);

        return appointmentRepository
                .findByPatientIdOrderByDateDescStartTimeDesc(
                        patientId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse updateStatus(
            Long id,
            UpdateAppointmentStatusRequest request
    ) {

        Appointment appointment =
                findAppointment(id);

        validateStatusTransition(
                appointment.getStatus(),
                request.getStatus()
        );

        AppointmentStatus newStatus = request.getStatus();
        appointment.setStatus(newStatus);

        AppointmentResponse response = mapToResponse(appointment);

        // Audit the status change
        Long patientUserId =
                appointment.getPatient().getUser().getId();

        auditService.log(
                patientUserId,
                "APPOINTMENT_STATUS_CHANGED",
                "Appointment",
                id,
                "Status changed to " + newStatus,
                null
        );

        // Publish event for CONFIRMED or CANCELLED — after commit the
        // NotificationDispatcher will create the notification + send WebSocket + email.
        if (newStatus == AppointmentStatus.CONFIRMED
                || newStatus == AppointmentStatus.CANCELLED) {

            String patientFirstName = appointment.getPatient().getUser().getFirstName();
            String patientLastName  = appointment.getPatient().getUser().getLastName();
            String patientName      = patientFirstName + " " + patientLastName;
            String patientEmail     = appointment.getPatient().getUser().getEmail();

            String doctorName = appointment.getDoctor().getUser().getFirstName()
                    + " " + appointment.getDoctor().getUser().getLastName();

            String dateStr = appointment.getDate().toString();

            NotificationType notificationType =
                    newStatus == AppointmentStatus.CONFIRMED
                            ? NotificationType.APPOINTMENT_CONFIRMED
                            : NotificationType.APPOINTMENT_CANCELLED;

            String title = newStatus == AppointmentStatus.CONFIRMED
                    ? "Appointment Confirmed"
                    : "Appointment Cancelled";

            String message = newStatus == AppointmentStatus.CONFIRMED
                    ? "Your appointment with Dr. " + doctorName + " on " + dateStr + " has been confirmed."
                    : "Your appointment with Dr. " + doctorName + " on " + dateStr + " has been cancelled.";

            eventPublisher.publishEvent(
                    new NotificationEvent(
                            patientUserId,
                            notificationType,
                            title,
                            message,
                            patientEmail,
                            patientName,
                            id
                    )
            );
        }

        return response;
    }

    private Doctor findDoctor(Long doctorId) {

        return doctorRepository
                .findById(doctorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Doctor not found with id: "
                                        + doctorId
                        )
                );
    }

    private Patient findPatient(Long patientId) {

        return patientRepository
                .findById(patientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Patient not found with id: "
                                        + patientId
                        )
                );
    }

    private DoctorBranch findActiveDoctorBranch(
            Long doctorId,
            Long branchId
    ) {

        DoctorBranch doctorBranch =
                doctorBranchRepository
                        .findByDoctorIdAndBranchId(
                                doctorId,
                                branchId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Doctor is not assigned to this branch"
                                )
                        );

        if (!doctorBranch.isEnabled()) {

            throw new IllegalStateException(
                    "Doctor is disabled in this branch"
            );
        }

        return doctorBranch;
    }

    private Appointment findAppointment(Long id) {

        return appointmentRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Appointment not found with id: "
                                        + id
                        )
                );
    }

    private void validateDateTime(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {

        LocalDateTime startDateTime =
                LocalDateTime.of(
                        date,
                        startTime
                );

        if (startDateTime.isBefore(
                LocalDateTime.now()
        )) {

            throw new IllegalArgumentException(
                    "Appointment cannot be in the past"
            );
        }

        if (!startTime.isBefore(endTime)) {

            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }
    }

    private void validateDoctorSchedule(
            Long doctorBranchId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {

        DayOfWeek dayOfWeek =
                date.getDayOfWeek();

        List<DoctorSchedule> schedules =
                doctorScheduleRepository
                        .findByDoctorBranchIdAndDayOfWeek(
                                doctorBranchId,
                                dayOfWeek
                        );

        boolean insideSchedule =
                schedules.stream()
                        .anyMatch(schedule ->
                                !startTime.isBefore(
                                        schedule.getStartTime()
                                )
                                        &&
                                        !endTime.isAfter(
                                                schedule.getEndTime()
                                        )
                        );

        if (!insideSchedule) {

            throw new IllegalArgumentException(
                    "Appointment time is outside doctor's schedule"
            );
        }
    }

    private void validateNoDoubleBooking(
            Long doctorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {

        List<AppointmentStatus> blockingStatuses =
                List.of(
                        AppointmentStatus.PENDING,
                        AppointmentStatus.CONFIRMED
                );

        boolean exists =
                appointmentRepository
                        .existsOverlappingAppointment(
                                doctorId,
                                date,
                                startTime,
                                endTime,
                                blockingStatuses
                        );

        if (exists) {

            throw new DuplicateResourceException(
                    "Doctor already has an appointment at this time"
            );
        }
    }

    private void validateStatusTransition(
            AppointmentStatus currentStatus,
            AppointmentStatus newStatus
    ) {

        if (currentStatus == newStatus) {

            throw new IllegalArgumentException(
                    "Appointment already has this status"
            );
        }

        boolean valid = switch (currentStatus) {

            case PENDING ->
                    newStatus == AppointmentStatus.CONFIRMED
                            || newStatus == AppointmentStatus.CANCELLED;

            case CONFIRMED ->
                    newStatus == AppointmentStatus.COMPLETED
                            || newStatus == AppointmentStatus.CANCELLED
                            || newStatus == AppointmentStatus.NO_SHOW;

            case COMPLETED, CANCELLED, NO_SHOW ->
                    false;
        };

        if (!valid) {

            throw new IllegalArgumentException(
                    "Invalid appointment status transition from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }
    }


    private Doctor findDoctorForUpdate(Long doctorId) {

        return doctorRepository
                .findByIdForUpdate(doctorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Doctor not found with id: " + doctorId
                        )
                );
    }


    private AppointmentResponse mapToResponse(
            Appointment appointment
    ) {

        Doctor doctor =
                appointment.getDoctor();

        Patient patient =
                appointment.getPatient();

        DoctorBranch doctorBranch =
                appointment.getDoctorBranch();

        String doctorName =
                doctor.getUser().getFirstName()
                        + " "
                        + doctor.getUser().getLastName();

        String patientName =
                patient.getUser().getFirstName()
                        + " "
                        + patient.getUser().getLastName();

        return AppointmentResponse.builder()
                .id(appointment.getId())

                .doctorId(doctor.getId())
                .doctorName(doctorName)

                .patientId(patient.getId())
                .patientName(patientName)

                .branchId(
                        doctorBranch
                                .getBranch()
                                .getId()
                )
                .branchName(
                        doctorBranch
                                .getBranch()
                                .getName()
                )

                .date(appointment.getDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())

                .status(appointment.getStatus())

                .notes(appointment.getNotes())

                .build();
    }
}