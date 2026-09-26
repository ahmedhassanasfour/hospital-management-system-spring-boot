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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/patient/files")
@RequiredArgsConstructor
@Tag(name = "Patient — Medical Files",
        description = "Upload, list, and download medical documents for the authenticated patient. Requires PATIENT role.")
public class PatientFileController {

    private final MedicalFileService medicalFileService;

    @Operation(summary = "Upload a medical document",
            description = "Uploads a patient-owned medical document (e.g., referral letter, scan report). " +
                          "Accepted file types and max size are enforced server-side (default max: 10 MB).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File uploaded — metadata returned"),
            @ApiResponse(responseCode = "400", description = "File validation failed (type or size)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — PATIENT role required")
    })
    @PostMapping(
            value = "/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public MedicalFileResponse uploadMedicalDocument(
            @Parameter(description = "Medical document file (PDF, image, etc.)")
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {

        return medicalFileService.uploadMedicalDocument(
                file,
                authentication.getName()
        );
    }

    @Operation(summary = "List my uploaded files",
            description = "Returns metadata for all files uploaded by the authenticated patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public List<MedicalFileResponse> getMyFiles(
            Authentication authentication
    ) {

        return medicalFileService.getMyFiles(
                authentication.getName()
        );
    }

    @Operation(summary = "Download a medical file",
            description = "Downloads a file owned by the authenticated patient. " +
                          "The response content-type is set to match the original file format.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File downloaded"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — file belongs to a different patient"),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @Parameter(description = "File ID") @PathVariable Long id,
            Authentication authentication
    ) throws MalformedURLException {

        Path path = medicalFileService.downloadForPatient(
                id,
                authentication.getName()
        );

        Resource resource =
                new UrlResource(path.toUri());

        String contentType;

        try {
            contentType = Files.probeContentType(path);
        } catch (Exception ex) {
            contentType =
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (contentType == null) {
            contentType =
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(contentType)
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + path.getFileName()
                                + "\""
                )
                .body(resource);
    }
}