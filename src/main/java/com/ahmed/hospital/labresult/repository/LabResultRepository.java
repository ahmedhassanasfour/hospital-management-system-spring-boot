package com.ahmed.hospital.labresult.repository;

import com.ahmed.hospital.labresult.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabResultRepository
        extends JpaRepository<LabResult, Long> {

    Optional<LabResult> findByLabOrderId(Long labOrderId);

    List<LabResult> findByLabOrderPatientIdOrderByResultDateDesc(
            Long patientId
    );

    boolean existsByLabOrderId(Long labOrderId);
}