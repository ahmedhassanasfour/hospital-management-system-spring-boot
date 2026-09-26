package com.ahmed.hospital.medicine.repository;

import com.ahmed.hospital.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}