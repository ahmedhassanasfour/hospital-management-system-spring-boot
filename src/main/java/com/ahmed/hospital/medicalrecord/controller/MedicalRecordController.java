package com.ahmed.hospital.medicalrecord.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.medicalrecord.dto.CreateMedicalRecordRequest;
import com.ahmed.hospital.medicalrecord.dto.MedicalRecordResponse;
import com.ahmed.hospital.medicalrecord.dto.UpdateMedicalRecordRequest;
import com.ahmed.hospital.medicalrecord.service.MedicalRecordService;
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
@RequestMapping("/api/admin/medical-records")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Medical Records",
        description = "Patient medical record management linked to appointments. Requires ADMIN role.")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @Operation(summary = "Create a medical record",
            description = "Creates a medical record for a completed appointment. " +
                          "Each appointment can have at most one medical record.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medical record created"),
            @ApiResponse(responseCode = "400", description = "Validation error or record already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/appointments/{appointmentId}")
    public ResponseEntity<MedicalRecordResponse> createMedicalRecord(
            @Parameter(description = "Appointment ID") @PathVariable Long appointmentId,
            @Valid @RequestBody CreateMedicalRecordRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        medicalRecordService.createMedicalRecord(
                                appointmentId,
                                request
                        )
                );
    }

    @Operation(summary = "Get medical record by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medical record found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Medical record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> getMedicalRecordById(
            @Parameter(description = "Medical record ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                medicalRecordService.getMedicalRecordById(id)
        );
    }

    @Operation(summary = "Get medical record by appointment ID",
            description = "Returns the medical record associated with a specific appointment, if one exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medical record found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Appointment or record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<MedicalRecordResponse> getByAppointmentId(
            @Parameter(description = "Appointment ID") @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(
                medicalRecordService.getByAppointmentId(appointmentId)
        );
    }

    @Operation(summary = "Get full medical history for a patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<MedicalRecordResponse>> getPatientMedicalHistory(
            @Parameter(description = "Patient ID") @PathVariable Long patientId
    ) {
        return ResponseEntity.ok(
                medicalRecordService.getPatientMedicalHistory(patientId)
        );
    }

    @Operation(summary = "Get all medical records created by a doctor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Records returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/doctors/{doctorId}")
    public ResponseEntity<List<MedicalRecordResponse>> getDoctorMedicalRecords(
            @Parameter(description = "Doctor ID") @PathVariable Long doctorId
    ) {
        return ResponseEntity.ok(
                medicalRecordService.getDoctorMedicalRecords(doctorId)
        );
    }

    @Operation(summary = "Update a medical record",
            description = "Updates the diagnosis, notes, or other details on an existing medical record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medical record updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Medical record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> updateMedicalRecord(
            @Parameter(description = "Medical record ID") @PathVariable Long id,
            @Valid @RequestBody UpdateMedicalRecordRequest request
    ) {
        return ResponseEntity.ok(
                medicalRecordService.updateMedicalRecord(
                        id,
                        request
                )
        );
    }
}