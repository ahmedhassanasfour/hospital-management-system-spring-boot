package com.ahmed.hospital.doctor.service;

import com.ahmed.hospital.branch.entity.Branch;
import com.ahmed.hospital.branch.repository.BranchRepository;
import com.ahmed.hospital.common.exception.DuplicateResourceException;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.doctor.dto.DoctorBranchResponse;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.entity.DoctorBranch;
import com.ahmed.hospital.doctor.repository.DoctorBranchRepository;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorBranchService {

    private final DoctorBranchRepository doctorBranchRepository;
    private final DoctorRepository doctorRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;

    @Transactional
    public DoctorBranchResponse assignDoctorToBranch(
            Long doctorId,
            Long branchId
    ) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Doctor not found with id: " + doctorId
                        )
                );

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Branch not found with id: " + branchId
                        )
                );

        var existingAssignment =
                doctorBranchRepository.findByDoctorIdAndBranchId(
                        doctorId,
                        branchId
                );

        if (existingAssignment.isPresent()) {

            DoctorBranch doctorBranch = existingAssignment.get();

            if (doctorBranch.isEnabled()) {
                throw new DuplicateResourceException(
                        "Doctor is already assigned to this branch"
                );
            }

            doctorBranch.setEnabled(true);

            auditService.log(
                    doctor.getUser().getId(),
                    "DOCTOR_BRANCH_ENABLED",
                    "DoctorBranch",
                    doctorBranch.getId(),
                    "Doctor " + doctorId + " re-enabled in branch " + branch.getName(),
                    null
            );

            return mapToResponse(doctorBranch);
        }

        DoctorBranch doctorBranch = DoctorBranch.builder()
                .doctor(doctor)
                .branch(branch)
                .enabled(true)
                .build();

        DoctorBranch saved = doctorBranchRepository.save(doctorBranch);

        auditService.log(
                doctor.getUser().getId(),
                "DOCTOR_BRANCH_ASSIGNED",
                "DoctorBranch",
                saved.getId(),
                "Doctor " + doctorId + " assigned to branch " + branch.getName(),
                null
        );

        return mapToResponse(saved);
    }

    @Transactional
    public DoctorBranchResponse disableDoctorFromBranch(
            Long doctorId,
            Long branchId
    ) {

        DoctorBranch doctorBranch =
                findAssignment(doctorId, branchId);

        doctorBranch.setEnabled(false);

        auditService.log(
                doctorBranch.getDoctor().getUser().getId(),
                "DOCTOR_BRANCH_DISABLED",
                "DoctorBranch",
                doctorBranch.getId(),
                "Doctor " + doctorId + " disabled from branch " + doctorBranch.getBranch().getName(),
                null
        );

        return mapToResponse(doctorBranch);
    }

    @Transactional
    public DoctorBranchResponse enableDoctorInBranch(
            Long doctorId,
            Long branchId
    ) {

        DoctorBranch doctorBranch =
                findAssignment(doctorId, branchId);

        doctorBranch.setEnabled(true);

        auditService.log(
                doctorBranch.getDoctor().getUser().getId(),
                "DOCTOR_BRANCH_ENABLED",
                "DoctorBranch",
                doctorBranch.getId(),
                "Doctor " + doctorId + " enabled in branch " + doctorBranch.getBranch().getName(),
                null
        );

        return mapToResponse(doctorBranch);
    }


    public List<DoctorBranchResponse> getDoctorBranches(Long doctorId) {

        doctorRepository.findById(doctorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Doctor not found with id: " + doctorId
                        )
                );

        return doctorBranchRepository.findByDoctorId(doctorId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DoctorBranchResponse> getBranchDoctors(Long branchId) {

        branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Branch not found with id: " + branchId
                        )
                );

        return doctorBranchRepository.findByBranchId(branchId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private DoctorBranchResponse mapToResponse(
            DoctorBranch doctorBranch
    ) {

        return DoctorBranchResponse.builder()
                .id(doctorBranch.getId())
                .doctorId(doctorBranch.getDoctor().getId())
                .branchId(doctorBranch.getBranch().getId())
                .enabled(doctorBranch.isEnabled())
                .build();
    }


    private DoctorBranch findAssignment(
            Long doctorId,
            Long branchId
    ) {

        return doctorBranchRepository
                .findByDoctorIdAndBranchId(doctorId, branchId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Doctor is not assigned to this branch"
                        )
                );
    }
}