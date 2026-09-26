package com.ahmed.hospital.doctor.repository;

import com.ahmed.hospital.doctor.entity.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DoctorRepository
        extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserEmail(String email);

    Optional<Doctor> findByUserId(Long userId);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Doctor d
        WHERE d.id = :doctorId
        """)
    Optional<Doctor> findByIdForUpdate(
            @Param("doctorId") Long doctorId
    );
}