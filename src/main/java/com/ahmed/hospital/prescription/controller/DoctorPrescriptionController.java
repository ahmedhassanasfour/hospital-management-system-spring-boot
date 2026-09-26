package com.ahmed.hospital.prescription.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.prescription.dto.PrescriptionResponse;
import com.ahmed.hospital.prescription.dto.PrescriptionRequest;
import com.ahmed.hospital.prescription.service.PrescriptionService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Doctor — Prescriptions",
        description = "Prescription creation and retrieval for the authenticated doctor. Requires DOCTOR role.")
public class DoctorPrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "Create a prescription",
            description = "Issues a prescription for a patient. " +
                          "The prescription is automatically linked to the authenticated doctor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prescription created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — DOCTOR role required"),
            @ApiResponse(responseCode = "404", description = "Patient or medicine not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(
            @Valid @RequestBody PrescriptionRequest request,
            Authentication authentication
    ) {

        return prescriptionService.create(
                request,
                authentication.getName()
        );
    }

    @Operation(summary = "Get a prescription by ID",
            description = "Returns a prescription only if it was issued by the authenticated doctor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prescription found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — prescription belongs to a different doctor"),
            @ApiResponse(responseCode = "404", description = "Prescription not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public PrescriptionResponse getById(
            @Parameter(description = "Prescription ID") @PathVariable Long id,
            Authentication authentication
    ) {

        return prescriptionService.getForDoctor(
                id,
                authentication.getName()
        );
    }
}