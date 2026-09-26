package com.ahmed.hospital.branch.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchResponse {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private boolean enabled;
}