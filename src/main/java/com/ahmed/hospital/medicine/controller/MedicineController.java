package com.ahmed.hospital.medicine.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.medicine.dto.MedicineRequest;
import com.ahmed.hospital.medicine.dto.MedicineResponse;
import com.ahmed.hospital.medicine.service.MedicineService;
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
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
@Tag(name = "Medicines",
        description = "Medicine catalogue management. Accessible to authenticated users; write operations are typically restricted to ADMIN.")
public class MedicineController {

    private final MedicineService medicineService;

    @Operation(summary = "Add a medicine to the catalogue")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medicine created"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate medicine",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineResponse create(
            @Valid @RequestBody MedicineRequest request) {

        return medicineService.create(request);
    }

    @Operation(summary = "Get medicine by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medicine found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Medicine not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public MedicineResponse getById(
            @Parameter(description = "Medicine ID") @PathVariable Long id) {

        return medicineService.getById(id);
    }

    @Operation(summary = "List all active medicines")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medicine list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public List<MedicineResponse> getAll() {

        return medicineService.getAll();
    }

    @Operation(summary = "Update a medicine",
            description = "Updates the name, dosage, or other details of an existing medicine.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medicine updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Medicine not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public MedicineResponse update(
            @Parameter(description = "Medicine ID") @PathVariable Long id,
            @Valid @RequestBody MedicineRequest request) {

        return medicineService.update(id, request);
    }

    @Operation(summary = "Deactivate a medicine",
            description = "Soft-deletes a medicine so it cannot be prescribed. " +
                          "Existing prescriptions are preserved.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Medicine deactivated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Medicine not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @Parameter(description = "Medicine ID") @PathVariable Long id) {

        medicineService.deactivate(id);
    }
}