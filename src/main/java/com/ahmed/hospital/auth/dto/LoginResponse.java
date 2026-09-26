package com.ahmed.hospital.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}