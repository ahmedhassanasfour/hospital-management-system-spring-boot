package com.ahmed.hospital.doctor.repository;

import com.ahmed.hospital.doctor.entity.DoctorBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorBranchRepository
        extends JpaRepository<DoctorBranch, Long> {

    boolean existsByDoctorIdAndBranchId(
            Long doctorId,
            Long branchId
    );

    Optional<DoctorBranch> findByDoctorIdAndBranchId(
            Long doctorId,
            Long branchId
    );

    List<DoctorBranch> findByDoctorId(Long doctorId);

    List<DoctorBranch> findByBranchId(Long branchId);
}