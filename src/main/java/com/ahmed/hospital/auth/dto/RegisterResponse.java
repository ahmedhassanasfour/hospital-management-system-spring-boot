package com.ahmed.hospital.auth.dto;

import com.ahmed.hospital.user.entity.Role;

public record RegisterResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {
}