package com.ahmed.hospital.labresult.controller;

import com.ahmed.hospital.labresult.dto.LabResultResponse;
import com.ahmed.hospital.labresult.service.LabResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient/lab-results")
@RequiredArgsConstructor
@Tag(name = "Patient — Lab Results",
        description = "Lab result history for the authenticated patient. Requires PATIENT role.")
public class PatientLabResultController {

    private final LabResultService labResultService;

    @Operation(summary = "Get my lab result history",
            description = "Returns all lab results associated with the authenticated patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lab result history returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — PATIENT role required")
    })
    @GetMapping
    public List<LabResultResponse> getMyHistory(
            Authentication authentication
    ) {

        return labResultService.getMyHistory(
                authentication.getName()
        );
    }
}