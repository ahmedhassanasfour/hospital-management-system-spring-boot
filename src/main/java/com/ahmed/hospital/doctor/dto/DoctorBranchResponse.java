package com.ahmed.hospital.doctor.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DoctorBranchResponse {

    private Long id;

    private Long doctorId;

    private Long branchId;

    private boolean enabled;
}