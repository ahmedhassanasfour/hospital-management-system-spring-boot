package com.ahmed.hospital.user.dto;

import com.ahmed.hospital.user.entity.Role;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {
}