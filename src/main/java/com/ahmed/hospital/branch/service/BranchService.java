package com.ahmed.hospital.branch.service;

import com.ahmed.hospital.branch.dto.BranchResponse;
import com.ahmed.hospital.branch.dto.CreateBranchRequest;
import com.ahmed.hospital.branch.dto.UpdateBranchRequest;
import com.ahmed.hospital.branch.entity.Branch;
import com.ahmed.hospital.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {

        validateDuplicateData(request.getName(), request.getEmail());

        Branch branch = Branch.builder()
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail().trim().toLowerCase())
                .enabled(true)
                .build();

        Branch savedBranch = branchRepository.save(branch);

        return mapToResponse(savedBranch);
    }

    public BranchResponse getBranchById(Long id) {

        Branch branch = findBranchById(id);

        return mapToResponse(branch);
    }

    public List<BranchResponse> getAllBranches() {

        return branchRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public BranchResponse updateBranch(Long id, UpdateBranchRequest request) {

        Branch branch = findBranchById(id);

        validateDuplicateDataForUpdate(
                id,
                request.getName(),
                request.getEmail()
        );

        branch.setName(request.getName().trim());
        branch.setAddress(request.getAddress().trim());
        branch.setPhone(request.getPhone().trim());
        branch.setEmail(request.getEmail().trim().toLowerCase());

        return mapToResponse(branch);
    }

    @Transactional
    public BranchResponse enableBranch(Long id) {

        Branch branch = findBranchById(id);

        branch.setEnabled(true);

        return mapToResponse(branch);
    }

    @Transactional
    public BranchResponse disableBranch(Long id) {

        Branch branch = findBranchById(id);

        branch.setEnabled(false);

        return mapToResponse(branch);
    }

    private Branch findBranchById(Long id) {

        return branchRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Branch not found with id: " + id)
                );
    }

    private void validateDuplicateData(String name, String email) {

        if (branchRepository.existsByNameIgnoreCase(name.trim())) {
            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "Branch name already exists: " + name
            );
        }

        if (branchRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "Branch email already exists: " + email
            );
        }
    }

    private void validateDuplicateDataForUpdate(
            Long id,
            String name,
            String email
    ) {

        branchRepository.findByNameIgnoreCase(name.trim())
                .ifPresent(existingBranch -> {
                    if (!existingBranch.getId().equals(id)) {
                        throw new com.ahmed.hospital.common.exception.BadRequestException(
                                "Branch name already exists: " + name
                        );
                    }
                });

        branchRepository.findByEmailIgnoreCase(email.trim())
                .ifPresent(existingBranch -> {
                    if (!existingBranch.getId().equals(id)) {
                        throw new com.ahmed.hospital.common.exception.BadRequestException(
                                "Branch email already exists: " + email
                        );
                    }
                });
    }

    private BranchResponse mapToResponse(Branch branch) {

        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .enabled(branch.isEnabled())
                .build();
    }
}