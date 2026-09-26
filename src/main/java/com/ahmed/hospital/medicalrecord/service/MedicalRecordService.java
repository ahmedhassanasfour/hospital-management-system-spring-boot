package com.ahmed.hospital.medicalrecord.service;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.appointment.entity.AppointmentStatus;
import com.ahmed.hospital.appointment.repository.AppointmentRepository;
import com.ahmed.hospital.common.exception.DuplicateResourceException;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.medicalrecord.dto.CreateMedicalRecordRequest;
import com.ahmed.hospital.medicalrecord.dto.MedicalRecordResponse;
import com.ahmed.hospital.medicalrecord.dto.UpdateMedicalRecordRequest;
import com.ahmed.hospital.medicalrecord.entity.MedicalRecord;
import com.ahmed.hospital.medicalrecord.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public MedicalRecordResponse createMedicalRecord(
            Long appointmentId,
            CreateMedicalRecordRequest request
    ) {

        Appointment appointment =
                findAppointment(appointmentId);

        validateAppointmentForMedicalRecord(
                appointment
        );

        if (medicalRecordRepository
                .existsByAppointmentId(appointmentId)) {

            throw new DuplicateResourceException(
                    "Medical record already exists for this appointment"
            );
        }

        MedicalRecord medicalRecord =
                MedicalRecord.builder()
                        .appointment(appointment)
                        .doctor(appointment.getDoctor())
                        .patient(appointment.getPatient())
                        .diagnosis(request.getDiagnosis())
                        .symptoms(request.getSymptoms())
                        .notes(request.getNotes())
                        .treatment(request.getTreatment())
                        .build();

        MedicalRecord saved =
                medicalRecordRepository.save(
                        medicalRecord
                );

        return mapToResponse(saved);
    }

    public MedicalRecordResponse getMedicalRecordById(
            Long id
    ) {

        MedicalRecord medicalRecord =
                findMedicalRecord(id);

        return mapToResponse(medicalRecord);
    }

    public MedicalRecordResponse getByAppointmentId(
            Long appointmentId
    ) {

        MedicalRecord medicalRecord =
                medicalRecordRepository
                        .findByAppointmentId(
                                appointmentId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Medical record not found for appointment: "
                                                + appointmentId
                                )
                        );

        return mapToResponse(medicalRecord);
    }

    public List<MedicalRecordResponse> getPatientMedicalHistory(
            Long patientId
    ) {

        return medicalRecordRepository
                .findByPatientIdOrderByIdDesc(
                        patientId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<MedicalRecordResponse> getDoctorMedicalRecords(
            Long doctorId
    ) {

        return medicalRecordRepository
                .findByDoctorIdOrderByIdDesc(
                        doctorId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MedicalRecordResponse updateMedicalRecord(
            Long id,
            UpdateMedicalRecordRequest request
    ) {

        MedicalRecord medicalRecord =
                findMedicalRecord(id);

        medicalRecord.setDiagnosis(
                request.getDiagnosis()
        );

        medicalRecord.setSymptoms(
                request.getSymptoms()
        );

        medicalRecord.setNotes(
                request.getNotes()
        );

        medicalRecord.setTreatment(
                request.getTreatment()
        );

        return mapToResponse(medicalRecord);
    }

    private Appointment findAppointment(
            Long appointmentId
    ) {

        return appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Appointment not found with id: "
                                        + appointmentId
                        )
                );
    }

    private MedicalRecord findMedicalRecord(
            Long id
    ) {

        return medicalRecordRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Medical record not found with id: "
                                        + id
                        )
                );
    }

    private void validateAppointmentForMedicalRecord(
            Appointment appointment
    ) {

        if (appointment.getStatus()
                == AppointmentStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Cannot create medical record for a cancelled appointment"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.NO_SHOW) {

            throw new IllegalStateException(
                    "Cannot create medical record for a no-show appointment"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.PENDING) {

            throw new IllegalStateException(
                    "Appointment must be confirmed before creating a medical record"
            );
        }
    }

    private MedicalRecordResponse mapToResponse(
            MedicalRecord medicalRecord
    ) {

        Doctor doctor =
                medicalRecord.getDoctor();

        Patient patient =
                medicalRecord.getPatient();

        String doctorName =
                doctor.getUser().getFirstName()
                        + " "
                        + doctor.getUser().getLastName();

        String patientName =
                patient.getUser().getFirstName()
                        + " "
                        + patient.getUser().getLastName();

        return MedicalRecordResponse.builder()
                .id(medicalRecord.getId())

                .appointmentId(
                        medicalRecord
                                .getAppointment()
                                .getId()
                )

                .doctorId(doctor.getId())
                .doctorName(doctorName)

                .patientId(patient.getId())
                .patientName(patientName)

                .diagnosis(
                        medicalRecord.getDiagnosis()
                )

                .symptoms(
                        medicalRecord.getSymptoms()
                )

                .notes(
                        medicalRecord.getNotes()
                )

                .treatment(
                        medicalRecord.getTreatment()
                )

                .build();
    }
}