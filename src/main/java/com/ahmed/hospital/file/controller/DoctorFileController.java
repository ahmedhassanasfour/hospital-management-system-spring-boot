package com.ahmed.hospital.file.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.file.dto.MedicalFileResponse;
import com.ahmed.hospital.file.service.MedicalFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/doctor/files")
@RequiredArgsConstructor
@Tag(name = "Doctor — Medical Files",
        description = "File attachments for prescriptions and lab results. Requires DOCTOR role.")
public class DoctorFileController {

    private final MedicalFileService medicalFileService;

    @Operation(summary = "Attach a file to a prescription",
            description = "Uploads a file (e.g., a scanned prescription image) and links it to the given prescription. " +
                          "The authenticated doctor must own the prescription.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "File attached — metadata returned"),
            @ApiResponse(responseCode = "400", description = "File validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — prescription belongs to a different doctor"),
            @ApiResponse(responseCode = "404", description = "Prescription not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(
            value = "/prescriptions/{prescriptionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalFileResponse uploadPrescriptionAttachment(
            @Parameter(description = "Prescription ID") @PathVariable Long prescriptionId,
            @Parameter(description = "File to attach")
            @RequestParam("file") @NotNull MultipartFile file,
            Authentication authentication
    ) {

        return medicalFileService.uploadPrescriptionAttachment(
                prescriptionId,
                file,
                authentication.getName()
        );
    }

    @Operation(summary = "Attach a file to a lab result",
            description = "Uploads a file (e.g., a lab report PDF) and links it to the given lab result. " +
                          "The authenticated doctor must own the lab order associated with the result.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "File attached — metadata returned"),
            @ApiResponse(responseCode = "400", description = "File validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — lab result belongs to a different doctor"),
            @ApiResponse(responseCode = "404", description = "Lab result not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(
            value = "/lab-results/{labResultId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalFileResponse uploadLabResultFile(
            @Parameter(description = "Lab result ID") @PathVariable Long labResultId,
            @Parameter(description = "File to attach")
            @RequestParam("file") @NotNull MultipartFile file,
            Authentication authentication
    ) {

        return medicalFileService.uploadLabResultFile(
                labResultId,
                file,
                authentication.getName()
        );
    }
}