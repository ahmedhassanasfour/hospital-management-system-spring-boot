package com.ahmed.hospital.appointment.controller;

import com.ahmed.hospital.appointment.dto.AppointmentResponse;
import com.ahmed.hospital.appointment.dto.CreateAppointmentRequest;
import com.ahmed.hospital.appointment.dto.UpdateAppointmentStatusRequest;
import com.ahmed.hospital.appointment.service.AppointmentService;
import com.ahmed.hospital.common.response.ErrorResponse;
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
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Appointments", description = "Appointment scheduling and status management. Requires ADMIN role.")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(
            summary = "Create an appointment",
            description = "Schedules an appointment for a patient with a doctor at a specific branch. " +
                          "Validates that the doctor is assigned to the branch and that the requested time slot is available."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment created"),
            @ApiResponse(responseCode = "400", description = "Validation error or slot unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Doctor, patient, or branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/doctors/{doctorId}/patients/{patientId}/branches/{branchId}")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId,
            @Parameter(description = "Patient ID") @PathVariable Long patientId,
            @Parameter(description = "Branch ID") @PathVariable Long branchId,
            @Valid @RequestBody CreateAppointmentRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        appointmentService.createAppointment(
                                doctorId,
                                patientId,
                                branchId,
                                request
                        )
                );
    }

    @Operation(summary = "Get appointment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @Parameter(description = "Appointment ID") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                appointmentService.getAppointmentById(id)
        );
    }

    @Operation(summary = "Get all appointments for a doctor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/doctors/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAppointments(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId
    ) {

        return ResponseEntity.ok(
                appointmentService.getDoctorAppointments(
                        doctorId
                )
        );
    }

    @Operation(summary = "Get all appointments for a patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(
            @Parameter(description = "Patient ID") @PathVariable Long patientId
    ) {

        return ResponseEntity.ok(
                appointmentService.getPatientAppointments(
                        patientId
                )
        );
    }

    @Operation(
            summary = "Update appointment status",
            description = "Transitions the appointment to a new status (e.g., CONFIRMED, CANCELLED, COMPLETED)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @Parameter(description = "Appointment ID") @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request
    ) {

        return ResponseEntity.ok(
                appointmentService.updateStatus(
                        id,
                        request
                )
        );
    }
}