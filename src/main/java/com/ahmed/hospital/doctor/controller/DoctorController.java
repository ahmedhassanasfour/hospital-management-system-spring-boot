package com.ahmed.hospital.doctor.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.doctor.dto.CreateDoctorRequest;
import com.ahmed.hospital.doctor.dto.DoctorResponse;
import com.ahmed.hospital.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
@Tag(name = "Admin — Doctors", description = "Doctor account management. Requires ADMIN role.")
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(summary = "Create a doctor account",
            description = "Registers a new doctor in the system with the DOCTOR role. The email must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Doctor created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
            @ApiResponse(responseCode = "409", description = "Email already in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponse createDoctor(
            @Valid @RequestBody CreateDoctorRequest request
    ) {
        return doctorService.createDoctor(request);
    }

    @Operation(summary = "Get doctor by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctor found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public DoctorResponse getDoctorById(
            @Parameter(description = "Doctor ID") @PathVariable Long id
    ) {
        return doctorService.getDoctorById(id);
    }

    @Operation(summary = "List all doctors")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctor list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public List<DoctorResponse> getAllDoctors() {
        return doctorService.getAllDoctors();
    }

}