package com.ahmed.hospital.prescription.service;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.appointment.repository.AppointmentRepository;
import com.ahmed.hospital.audit.service.AuditService;
import com.ahmed.hospital.common.event.NotificationEvent;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.medicine.entity.Medicine;
import com.ahmed.hospital.medicine.repository.MedicineRepository;
import com.ahmed.hospital.notification.entity.NotificationType;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.patient.repository.PatientRepository;
import com.ahmed.hospital.prescription.dto.PrescriptionItemRequest;
import com.ahmed.hospital.prescription.dto.PrescriptionItemResponse;
import com.ahmed.hospital.prescription.dto.PrescriptionRequest;
import com.ahmed.hospital.prescription.dto.PrescriptionResponse;
import com.ahmed.hospital.prescription.entity.Prescription;
import com.ahmed.hospital.prescription.entity.PrescriptionItem;
import com.ahmed.hospital.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    public PrescriptionResponse create(
            PrescriptionRequest request,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        Appointment appointment = appointmentRepository
                .findById(request.appointmentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Appointment not found"
                        ));

        // Make sure the authenticated doctor owns this appointment
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException(
                    "You are not allowed to create a prescription for this appointment"
            );
        }

        // One prescription per appointment
        if (prescriptionRepository
                .existsByAppointmentId(appointment.getId())) {

            throw new IllegalArgumentException(
                    "Prescription already exists for this appointment"
            );
        }

        Prescription prescription = Prescription.builder()
                .patient(appointment.getPatient())
                .doctor(doctor)
                .appointment(appointment)
                .notes(request.notes())
                .build();

        for (PrescriptionItemRequest itemRequest : request.items()) {

            Medicine medicine = medicineRepository
                    .findById(itemRequest.medicineId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Medicine not found: "
                                            + itemRequest.medicineId()
                            ));

            if (!medicine.isActive()) {
                throw new IllegalArgumentException(
                        "Medicine is inactive: "
                                + medicine.getName()
                );
            }

            PrescriptionItem item = PrescriptionItem.builder()
                    .medicine(medicine)
                    .dosage(itemRequest.dosage())
                    .frequency(itemRequest.frequency())
                    .duration(itemRequest.duration())
                    .instructions(itemRequest.instructions())
                    .build();

            prescription.addItem(item);
        }

        Prescription savedPrescription =
                prescriptionRepository.save(prescription);

        // ── Audit ──────────────────────────────────────────────────────────────────
        auditService.log(
                doctor.getUser().getId(),
                "PRESCRIPTION_CREATED",
                "Prescription",
                savedPrescription.getId(),
                "Prescription created for patient "
                        + appointment.getPatient().getId()
                        + " for appointment " + appointment.getId(),
                null
        );

        // ── Event: notify patient after this transaction commits ───────────────────
        // Extract all data from managed entities now (inside the tx).
        Patient patient = appointment.getPatient();
        Long patientUserId = patient.getUser().getId();
        String patientName = patient.getUser().getFirstName()
                + " " + patient.getUser().getLastName();
        String patientEmail = patient.getUser().getEmail();
        String doctorName = doctor.getUser().getFirstName()
                + " " + doctor.getUser().getLastName();

        eventPublisher.publishEvent(
                new NotificationEvent(
                        patientUserId,
                        NotificationType.PRESCRIPTION_CREATED,
                        "New Prescription Available",
                        "Dr. " + doctorName + " has issued a prescription for you. "
                                + "Please log in to view the details.",
                        patientEmail,
                        patientName,
                        savedPrescription.getId()
                )
        );

        return toResponse(savedPrescription);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getForDoctor(
            Long prescriptionId,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        Prescription prescription = prescriptionRepository
                .findById(prescriptionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Prescription not found"
                        ));

        // Doctor can only access his own prescriptions
        if (!prescription.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException(
                    "You are not allowed to access this prescription"
            );
        }

        return toResponse(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getMyHistory(
            String authenticatedEmail
    ) {

        Patient patient = patientRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        return prescriptionRepository
                .findByPatientIdOrderByPrescribedAtDesc(patient.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PrescriptionResponse toResponse(
            Prescription prescription
    ) {

        List<PrescriptionItemResponse> items =
                prescription.getItems()
                        .stream()
                        .map(item -> new PrescriptionItemResponse(
                                item.getId(),
                                item.getMedicine().getId(),
                                item.getMedicine().getName(),
                                item.getDosage(),
                                item.getFrequency(),
                                item.getDuration(),
                                item.getInstructions()
                        ))
                        .toList();

        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPatient().getId(),
                prescription.getDoctor().getId(),
                prescription.getAppointment().getId(),
                prescription.getNotes(),
                prescription.getPrescribedAt(),
                items
        );
    }
}