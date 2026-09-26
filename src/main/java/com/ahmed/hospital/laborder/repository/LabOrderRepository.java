package com.ahmed.hospital.laborder.repository;

import com.ahmed.hospital.laborder.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabOrderRepository
        extends JpaRepository<LabOrder, Long> {

    List<LabOrder> findByPatientIdOrderByOrderedAtDesc(
            Long patientId
    );

    List<LabOrder> findByDoctorIdOrderByOrderedAtDesc(
            Long doctorId
    );

    List<LabOrder> findByAppointmentIdOrderByOrderedAtDesc(
            Long appointmentId
    );
}