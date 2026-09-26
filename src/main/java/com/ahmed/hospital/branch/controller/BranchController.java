package com.ahmed.hospital.branch.controller;

import com.ahmed.hospital.branch.dto.BranchResponse;
import com.ahmed.hospital.branch.dto.CreateBranchRequest;
import com.ahmed.hospital.branch.dto.UpdateBranchRequest;
import com.ahmed.hospital.branch.service.BranchService;
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
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Branches", description = "Hospital branch management. Requires ADMIN role.")
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "Create a branch",
            description = "Creates a new hospital branch. Branch is enabled by default.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Branch created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    })
    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody CreateBranchRequest request
    ) {

        BranchResponse response = branchService.createBranch(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get branch by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> getBranchById(
            @Parameter(description = "Branch ID") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                branchService.getBranchById(id)
        );
    }

    @Operation(summary = "List all branches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ResponseEntity<List<BranchResponse>> getAllBranches() {

        return ResponseEntity.ok(
                branchService.getAllBranches()
        );
    }

    @Operation(summary = "Update branch details",
            description = "Updates the name, address, or other details of an existing branch.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> updateBranch(
            @Parameter(description = "Branch ID") @PathVariable Long id,
            @Valid @RequestBody UpdateBranchRequest request
    ) {

        return ResponseEntity.ok(
                branchService.updateBranch(id, request)
        );
    }

    @Operation(summary = "Enable a branch",
            description = "Marks a disabled branch as active, allowing it to accept appointments.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch enabled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/enable")
    public ResponseEntity<BranchResponse> enableBranch(
            @Parameter(description = "Branch ID") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                branchService.enableBranch(id)
        );
    }

    @Operation(summary = "Disable a branch",
            description = "Marks a branch as inactive. Existing appointments are not affected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch disabled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Branch not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/disable")
    public ResponseEntity<BranchResponse> disableBranch(
            @Parameter(description = "Branch ID") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                branchService.disableBranch(id)
        );
    }
}