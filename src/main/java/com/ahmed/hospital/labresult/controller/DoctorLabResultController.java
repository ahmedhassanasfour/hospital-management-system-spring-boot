package com.ahmed.hospital.labresult.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.labresult.dto.LabResultRequest;
import com.ahmed.hospital.labresult.dto.LabResultResponse;
import com.ahmed.hospital.labresult.service.LabResultService;
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
@RequestMapping("/api/doctor/lab-results")
@RequiredArgsConstructor
@Tag(name = "Doctor — Lab Results",
        description = "Recording and retrieval of lab test results. Requires DOCTOR role.")
public class DoctorLabResultController {

    private final LabResultService labResultService;

    @Operation(summary = "Record a lab result",
            description = "Attaches a result to an existing lab order. " +
                          "The owning doctor must be the one recording the result.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lab result recorded"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — DOCTOR role required, must own the lab order"),
            @ApiResponse(responseCode = "404", description = "Lab order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/lab-orders/{labOrderId}")
    @ResponseStatus(HttpStatus.CREATED)
    public LabResultResponse create(
            @Parameter(description = "Lab order ID") @PathVariable Long labOrderId,
            @Valid @RequestBody LabResultRequest request,
            Authentication authentication
    ) {

        return labResultService.create(
                labOrderId,
                request,
                authentication.getName()
        );
    }

    @Operation(summary = "Get a lab result by ID",
            description = "Returns the lab result only if it belongs to a lab order owned by the authenticated doctor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lab result found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — result belongs to a different doctor"),
            @ApiResponse(responseCode = "404", description = "Lab result not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public LabResultResponse getById(
            @Parameter(description = "Lab result ID") @PathVariable Long id,
            Authentication authentication
    ) {

        return labResultService.getForDoctor(
                id,
                authentication.getName()
        );
    }
}