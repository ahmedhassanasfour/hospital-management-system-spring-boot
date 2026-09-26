package com.ahmed.hospital.doctor.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.doctor.dto.DoctorBranchResponse;
import com.ahmed.hospital.doctor.service.DoctorBranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Doctor Branch Assignments",
        description = "Assign doctors to branches and manage their active status at each branch. Requires ADMIN role.")
public class DoctorBranchController {

    private final DoctorBranchService doctorBranchService;

    @Operation(summary = "Assign a doctor to a branch",
            description = "Creates a doctor–branch assignment, enabling the doctor to see patients at that branch.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Doctor assigned to branch"),
            @ApiResponse(responseCode = "400", description = "Already assigned",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Doctor or branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/doctors/{doctorId}/branches/{branchId}")
    public ResponseEntity<DoctorBranchResponse> assignDoctorToBranch(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId,
            @Parameter(description = "Branch ID") @PathVariable Long branchId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        doctorBranchService.assignDoctorToBranch(
                                doctorId,
                                branchId
                        )
                );
    }

    @Operation(summary = "Enable a doctor at a branch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctor enabled at branch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Assignment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/doctors/{doctorId}/branches/{branchId}/enable")
    public ResponseEntity<DoctorBranchResponse> enableDoctorInBranch(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId,
            @Parameter(description = "Branch ID") @PathVariable Long branchId
    ) {

        return ResponseEntity.ok(
                doctorBranchService.enableDoctorInBranch(
                        doctorId,
                        branchId
                )
        );
    }

    @Operation(summary = "Disable a doctor at a branch",
            description = "Prevents the doctor from accepting new appointments at this branch. Existing appointments are not cancelled.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctor disabled at branch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Assignment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/doctors/{doctorId}/branches/{branchId}/disable")
    public ResponseEntity<DoctorBranchResponse> disableDoctorFromBranch(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId,
            @Parameter(description = "Branch ID") @PathVariable Long branchId
    ) {

        return ResponseEntity.ok(
                doctorBranchService.disableDoctorFromBranch(
                        doctorId,
                        branchId
                )
        );
    }

    @Operation(summary = "List all branches for a doctor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/doctors/{doctorId}/branches")
    public ResponseEntity<List<DoctorBranchResponse>> getDoctorBranches(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId
    ) {

        return ResponseEntity.ok(
                doctorBranchService.getDoctorBranches(doctorId)
        );
    }

    @Operation(summary = "List all doctors at a branch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctor list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/branches/{branchId}/doctors")
    public ResponseEntity<List<DoctorBranchResponse>> getBranchDoctors(
            @Parameter(description = "Branch ID") @PathVariable Long branchId
    ) {

        return ResponseEntity.ok(
                doctorBranchService.getBranchDoctors(branchId)
        );
    }
}