package com.ahmed.hospital.prescription.repository;

import com.ahmed.hospital.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository
        extends JpaRepository<Prescription, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    Optional<Prescription> findByAppointmentId(Long appointmentId);

    List<Prescription> findByPatientIdOrderByPrescribedAtDesc(Long patientId);

    List<Prescription> findByDoctorIdOrderByPrescribedAtDesc(Long doctorId);
}