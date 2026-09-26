package com.ahmed.hospital.user.controller;

import com.ahmed.hospital.user.dto.UserResponse;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operations on the currently authenticated user's own profile. Requires a valid Bearer token.")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = "Returns the id, email, name, and role of the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Token missing or expired")
    })
    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {

        User user = userService.findByEmail(
                authentication.getName()
        );

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}