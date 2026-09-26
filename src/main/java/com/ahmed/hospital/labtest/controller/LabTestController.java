package com.ahmed.hospital.labtest.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.labtest.dto.LabTestRequest;
import com.ahmed.hospital.labtest.dto.LabTestResponse;
import com.ahmed.hospital.labtest.service.LabTestService;
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
@RequestMapping("/api/admin/lab-tests")
@RequiredArgsConstructor
@Tag(name = "Admin — Lab Tests",
        description = "Lab test catalogue management. Requires ADMIN role.")
public class LabTestController {

    private final LabTestService labTestService;

    @Operation(summary = "Add a lab test to the catalogue",
            description = "Creates a new lab test type that doctors can order for patients.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lab test created"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate test name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabTestResponse create(
            @Valid @RequestBody LabTestRequest request
    ) {
        return labTestService.create(request);
    }

    @Operation(summary = "Get lab test by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lab test found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Lab test not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public LabTestResponse getById(
            @Parameter(description = "Lab test ID") @PathVariable Long id
    ) {
        return labTestService.getById(id);
    }

    @Operation(summary = "List all active lab tests")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lab test list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public List<LabTestResponse> getAll() {
        return labTestService.getAll();
    }

    @Operation(summary = "Update a lab test",
            description = "Updates the name, description, or price of an existing lab test.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lab test updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Lab test not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public LabTestResponse update(
            @Parameter(description = "Lab test ID") @PathVariable Long id,
            @Valid @RequestBody LabTestRequest request
    ) {
        return labTestService.update(id, request);
    }

    @Operation(summary = "Deactivate a lab test",
            description = "Soft-deletes a lab test so it cannot be ordered, " +
                          "but existing lab orders referencing it are preserved.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lab test deactivated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Lab test not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @Parameter(description = "Lab test ID") @PathVariable Long id
    ) {
        labTestService.deactivate(id);
    }
}