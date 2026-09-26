package com.ahmed.hospital.medicalrecord.repository;

import com.ahmed.hospital.medicalrecord.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository
        extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByAppointmentId(
            Long appointmentId
    );

    boolean existsByAppointmentId(
            Long appointmentId
    );

    List<MedicalRecord> findByPatientIdOrderByIdDesc(
            Long patientId
    );

    List<MedicalRecord> findByDoctorIdOrderByIdDesc(
            Long doctorId
    );
}