package com.ahmed.hospital.doctor.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.doctor.dto.CreateDoctorScheduleRequest;
import com.ahmed.hospital.doctor.dto.DoctorScheduleResponse;
import com.ahmed.hospital.doctor.dto.UpdateDoctorScheduleRequest;
import com.ahmed.hospital.doctor.service.DoctorScheduleService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Doctor Schedules",
        description = "Manage doctor availability schedules at each branch. Requires ADMIN role.")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @Operation(summary = "Create a doctor schedule slot",
            description = "Defines a recurring weekly availability slot for a doctor at a specific branch " +
                          "(e.g., every Monday 09:00–17:00). The doctor must already be assigned to the branch.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Schedule created"),
            @ApiResponse(responseCode = "400", description = "Validation error or conflicting slot",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Doctor or branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/doctors/{doctorId}/branches/{branchId}/schedules")
    public ResponseEntity<DoctorScheduleResponse> createSchedule(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId,
            @Parameter(description = "Branch ID") @PathVariable Long branchId,
            @Valid @RequestBody CreateDoctorScheduleRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        doctorScheduleService.createSchedule(
                                doctorId,
                                branchId,
                                request
                        )
                );
    }

    @Operation(summary = "List schedules for a doctor at a branch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Doctor or branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/doctors/{doctorId}/branches/{branchId}/schedules")
    public ResponseEntity<List<DoctorScheduleResponse>> getDoctorSchedules(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId,
            @Parameter(description = "Branch ID") @PathVariable Long branchId
    ) {

        return ResponseEntity.ok(
                doctorScheduleService.getDoctorSchedules(
                        doctorId,
                        branchId
                )
        );
    }

    @Operation(summary = "Get schedule by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Schedule not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/doctor-schedules/{id}")
    public ResponseEntity<DoctorScheduleResponse> getScheduleById(
            @Parameter(description = "Schedule ID") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                doctorScheduleService.getScheduleById(id)
        );
    }

    @Operation(summary = "Update a schedule slot",
            description = "Modifies the day, start time, or end time of an existing schedule entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or conflicting slot",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Schedule not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/doctor-schedules/{id}")
    public ResponseEntity<DoctorScheduleResponse> updateSchedule(
            @Parameter(description = "Schedule ID") @PathVariable Long id,
            @Valid @RequestBody UpdateDoctorScheduleRequest request
    ) {

        return ResponseEntity.ok(
                doctorScheduleService.updateSchedule(
                        id,
                        request
                )
        );
    }

    @Operation(summary = "Delete a schedule slot")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Schedule deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Schedule not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/doctor-schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @Parameter(description = "Schedule ID") @PathVariable Long id
    ) {

        doctorScheduleService.deleteSchedule(id);

        return ResponseEntity.noContent().build();
    }
}