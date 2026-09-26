package com.ahmed.hospital.labresult.service;

import com.ahmed.hospital.audit.service.AuditService;
import com.ahmed.hospital.common.event.NotificationEvent;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.laborder.entity.LabOrder;
import com.ahmed.hospital.laborder.entity.LabOrderStatus;
import com.ahmed.hospital.laborder.repository.LabOrderRepository;
import com.ahmed.hospital.labresult.dto.LabResultRequest;
import com.ahmed.hospital.labresult.dto.LabResultResponse;
import com.ahmed.hospital.labresult.entity.LabResult;
import com.ahmed.hospital.labresult.repository.LabResultRepository;
import com.ahmed.hospital.notification.entity.NotificationType;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.patient.repository.PatientRepository;
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
public class LabResultService {

    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    public LabResultResponse create(
            Long labOrderId,
            LabResultRequest request,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        LabOrder labOrder = labOrderRepository
                .findById(labOrderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab order not found"
                        ));

        // Only the doctor who created the order can add the result
        if (!labOrder.getDoctor().getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to add a result to this lab order"
            );
        }

        if (labOrder.getStatus() == LabOrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot add result to a cancelled lab order"
            );
        }

        if (labResultRepository.existsByLabOrderId(labOrderId)) {
            throw new IllegalArgumentException(
                    "Lab result already exists"
            );
        }

        LabResult result = LabResult.builder()
                .labOrder(labOrder)
                .resultValue(request.resultValue())
                .referenceRange(request.referenceRange())
                .notes(request.notes())
                .build();

        LabResult savedResult =
                labResultRepository.save(result);

        // Creating a result means the lab order is completed
        labOrder.markCompleted();

        // ── Audit ──────────────────────────────────────────────────────────────────
        auditService.log(
                doctor.getUser().getId(),
                "LAB_RESULT_CREATED",
                "LabResult",
                savedResult.getId(),
                "Lab result recorded for lab order " + labOrderId,
                null
        );

        // ── Event: notify the patient after this transaction commits ───────────────
        // Collect all patient data from the managed entity NOW (inside the tx),
        // pass only primitives into the event record.
        Patient patient = labOrder.getPatient();
        Long patientUserId = patient.getUser().getId();
        String patientName = patient.getUser().getFirstName()
                + " " + patient.getUser().getLastName();
        String patientEmail = patient.getUser().getEmail();
        String testName = labOrder.getLabTest().getName();

        eventPublisher.publishEvent(
                new NotificationEvent(
                        patientUserId,
                        NotificationType.LAB_RESULT_AVAILABLE,
                        "Lab Result Available",
                        "Your lab result for '" + testName
                                + "' is now available. Please log in to view your results.",
                        patientEmail,
                        patientName,
                        savedResult.getId()
                )
        );

        return toResponse(savedResult);
    }

    @Transactional(readOnly = true)
    public LabResultResponse getForDoctor(
            Long resultId,
            String authenticatedEmail
    ) {

        Doctor doctor = doctorRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        ));

        LabResult result = labResultRepository
                .findById(resultId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lab result not found"
                        ));

        if (!result.getLabOrder().getDoctor().getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this lab result"
            );
        }

        return toResponse(result);
    }

    @Transactional(readOnly = true)
    public List<LabResultResponse> getMyHistory(
            String authenticatedEmail
    ) {

        Patient patient = patientRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        return labResultRepository
                .findByLabOrderPatientIdOrderByResultDateDesc(
                        patient.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LabResultResponse toResponse(
            LabResult result
    ) {

        LabOrder labOrder = result.getLabOrder();

        return new LabResultResponse(
                result.getId(),
                labOrder.getId(),
                labOrder.getPatient().getId(),
                labOrder.getDoctor().getId(),
                labOrder.getLabTest().getId(),
                labOrder.getLabTest().getName(),
                result.getResultValue(),
                result.getReferenceRange(),
                result.getNotes(),
                result.getResultDate()
        );
    }
}