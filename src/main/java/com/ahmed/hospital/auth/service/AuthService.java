package com.ahmed.hospital.auth.service;

import com.ahmed.hospital.audit.service.AuditService;
import com.ahmed.hospital.auth.security.JwtService;
import com.ahmed.hospital.common.exception.BadRequestException;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.common.exception.UnauthorizedException;
import com.ahmed.hospital.auth.dto.LoginRequest;
import com.ahmed.hospital.auth.dto.LoginResponse;
import com.ahmed.hospital.auth.dto.RegisterRequest;
import com.ahmed.hospital.auth.dto.RegisterResponse;
import com.ahmed.hospital.user.entity.Role;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException(
                    "Email already exists"
            );
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.PATIENT)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Audit: new user registered — no password logged
        auditService.log(
                savedUser.getId(),
                "USER_REGISTERED",
                "User",
                savedUser.getId(),
                "New patient account registered for " + savedUser.getEmail(),
                null
        );

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole()
        );
    }

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Authentication passed — look up user for the audit entry
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        String accessToken =
                jwtService.generateAccessToken(
                        request.email()
                );

        String refreshToken =
                jwtService.generateRefreshToken(
                        request.email()
                );

        // Audit: successful login — no token logged
        auditService.log(
                user.getId(),
                "USER_LOGIN",
                "User",
                user.getId(),
                "Successful login for " + user.getEmail(),
                null
        );

        return new LoginResponse(
                accessToken,
                refreshToken
        );
    }

    public LoginResponse refreshToken(String refreshToken) {

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException(
                    "Invalid refresh token"
            );
        }

        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        if (!user.isEnabled()) {
            throw new UnauthorizedException(
                    "User is disabled"
            );
        }

        String newAccessToken =
                jwtService.generateAccessToken(email);

        String newRefreshToken =
                jwtService.generateRefreshToken(email);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken
        );
    }
}