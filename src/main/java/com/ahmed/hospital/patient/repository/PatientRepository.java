package com.ahmed.hospital.patient.repository;

import com.ahmed.hospital.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository
        extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    boolean existsByNationalId(String nationalId);

    boolean existsByUserId(Long userId);

    Optional<Patient> findByUserEmail(String email);
}