package com.ahmed.hospital.patient.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.patient.dto.CreatePatientRequest;
import com.ahmed.hospital.patient.dto.PatientResponse;
import com.ahmed.hospital.patient.service.PatientService;
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
@RequestMapping("/api/admin/patients")
@RequiredArgsConstructor
@Tag(name = "Admin — Patients", description = "Patient account management. Requires ADMIN role.")
public class PatientController {

    private final PatientService patientService;

    @Operation(summary = "Create a patient account",
            description = "Registers a new patient in the system. The email must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Patient created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
            @ApiResponse(responseCode = "409", description = "Email already in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse createPatient(
            @Valid @RequestBody CreatePatientRequest request
    ) {
        return patientService.createPatient(request);
    }

    @Operation(summary = "Get patient by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public PatientResponse getPatientById(
            @Parameter(description = "Patient ID") @PathVariable Long id
    ) {
        return patientService.getPatientById(id);
    }

    @Operation(summary = "List all patients")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public List<PatientResponse> getAllPatients() {
        return patientService.getAllPatients();
    }
}