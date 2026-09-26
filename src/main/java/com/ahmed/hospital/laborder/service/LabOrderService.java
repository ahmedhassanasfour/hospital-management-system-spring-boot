package com.ahmed.hospital.laborder.service;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.appointment.repository.AppointmentRepository;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.laborder.dto.LabOrderRequest;
import com.ahmed.hospital.laborder.dto.LabOrderResponse;
import com.ahmed.hospital.laborder.entity.LabOrder;
import com.ahmed.hospital.laborder.entity.LabOrderStatus;
import com.ahmed.hospital.laborder.repository.LabOrderRepository;
import com.ahmed.hospital.labtest.entity.LabTest;
import com.ahmed.hospital.labtest.repository.LabTestRepository;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final LabTestRepository labTestRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public LabOrderResponse create(
            LabOrderRequest request,
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

        // The authenticated doctor must own the appointment
        if (!appointment.getDoctor().getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to create a lab order for this appointment"
            );
        }

        LabTest labTest = labTestRepository
                .findById(request.labTestId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab test not found"
                        ));

        if (!labTest.isActive()) {
            throw new IllegalArgumentException(
                    "Lab test is inactive"
            );
        }

        Patient patient = appointment.getPatient();

        LabOrder labOrder = LabOrder.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .labTest(labTest)
                .status(LabOrderStatus.ORDERED)
                .notes(request.notes())
                .build();

        return toResponse(
                labOrderRepository.save(labOrder)
        );
    }

    @Transactional(readOnly = true)
    public LabOrderResponse getForDoctor(
            Long id,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        LabOrder labOrder = labOrderRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab order not found"
                        ));

        if (!labOrder.getDoctor().getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this lab order"
            );
        }

        return toResponse(labOrder);
    }

    @Transactional(readOnly = true)
    public List<LabOrderResponse> getMyHistory(
            String authenticatedEmail
    ) {

        Patient patient = patientRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        return labOrderRepository
                .findByPatientIdOrderByOrderedAtDesc(
                        patient.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LabOrderResponse updateStatus(
            Long id,
            LabOrderStatus status,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        LabOrder labOrder = labOrderRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab order not found"
                        ));

        // IDOR protection: only the owning doctor can update status
        if (!labOrder.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException(
                    "You are not allowed to update this lab order"
            );
        }

        if (labOrder.getStatus() == LabOrderStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Completed lab order cannot be modified"
            );
        }

        if (labOrder.getStatus() == LabOrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cancelled lab order cannot be modified"
            );
        }

        switch (status) {

            case IN_PROGRESS ->
                    labOrder.markInProgress();

            case COMPLETED ->
                    labOrder.markCompleted();

            case CANCELLED ->
                    labOrder.cancel();

            case ORDERED ->
                    throw new IllegalArgumentException(
                            "Cannot change status back to ORDERED"
                    );
        }

        return toResponse(labOrder);
    }

    private LabOrderResponse toResponse(
            LabOrder labOrder
    ) {

        return new LabOrderResponse(
                labOrder.getId(),
                labOrder.getPatient().getId(),
                labOrder.getDoctor().getId(),
                labOrder.getAppointment().getId(),
                labOrder.getLabTest().getId(),
                labOrder.getLabTest().getName(),
                labOrder.getStatus(),
                labOrder.getNotes(),
                labOrder.getOrderedAt(),
                labOrder.getCompletedAt()
        );
    }
}