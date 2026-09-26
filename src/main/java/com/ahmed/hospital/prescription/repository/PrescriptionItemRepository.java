package com.ahmed.hospital.prescription.repository;

import com.ahmed.hospital.prescription.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;



public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

}