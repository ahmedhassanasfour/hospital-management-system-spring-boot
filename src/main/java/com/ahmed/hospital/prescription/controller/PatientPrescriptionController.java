package com.ahmed.hospital.prescription.controller;

import com.ahmed.hospital.prescription.dto.PrescriptionResponse;
import com.ahmed.hospital.prescription.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Patient — Prescriptions",
        description = "Prescription history for the authenticated patient. Requires PATIENT role.")
public class PatientPrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "Get my prescriptions",
            description = "Returns all prescriptions issued to the authenticated patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prescription list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — PATIENT role required")
    })
    @GetMapping
    public List<PrescriptionResponse> getMyPrescriptions(
            Authentication authentication
    ) {

        return prescriptionService.getMyHistory(
                authentication.getName()
        );
    }
}