package com.ahmed.hospital.auth.controller;

import com.ahmed.hospital.auth.service.AuthService;
import com.ahmed.hospital.auth.dto.LoginRequest;
import com.ahmed.hospital.auth.dto.LoginResponse;
import com.ahmed.hospital.auth.dto.RefreshTokenRequest;
import com.ahmed.hospital.auth.dto.RegisterRequest;
import com.ahmed.hospital.auth.dto.RegisterResponse;
import com.ahmed.hospital.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public endpoints for registration, login, and token refresh. No Bearer token required.")
@SecurityRequirements   // override global bearer requirement — these are public
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new patient account",
            description = "Creates a new user account with the PATIENT role. " +
                          "The email must be unique across the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @Operation(
            summary = "Login with email and password",
            description = "Authenticates the user and returns a short-lived access token " +
                          "(15 min) and a long-lived refresh token (7 days). " +
                          "Use the access token as: Authorization: Bearer <accessToken>."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — tokens returned"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @Operation(
            summary = "Refresh an expired access token",
            description = "Exchanges a valid refresh token for a new access token and refresh token pair. " +
                          "The old refresh token is invalidated after this call."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued"),
            @ApiResponse(responseCode = "400", description = "Validation error or malformed token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token expired or invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public LoginResponse refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authService.refreshToken(
                request.refreshToken()
        );
    }
}