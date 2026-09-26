package com.ahmed.hospital.labtest.repository;

import com.ahmed.hospital.labtest.entity.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabTestRepository
        extends JpaRepository<LabTest, Long> {

    Optional<LabTest> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}