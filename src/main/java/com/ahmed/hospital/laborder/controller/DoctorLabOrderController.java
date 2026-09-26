package com.ahmed.hospital.laborder.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.laborder.dto.LabOrderRequest;
import com.ahmed.hospital.laborder.dto.LabOrderResponse;
import com.ahmed.hospital.laborder.entity.LabOrderStatus;
import com.ahmed.hospital.laborder.service.LabOrderService;
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
@RequestMapping("/api/doctor/lab-orders")
@RequiredArgsConstructor
@Tag(name = "Doctor — Lab Orders",
        description = "Lab test ordering for patients. Requires DOCTOR role. " +
                      "Each doctor can only access and modify their own lab orders.")
public class DoctorLabOrderController {

    private final LabOrderService labOrderService;

    @Operation(summary = "Order a lab test for a patient",
            description = "Creates a lab order for a specific test. The order is automatically associated " +
                          "with the authenticated doctor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lab order created"),
            @ApiResponse(responseCode = "400", description = "Validation error or inactive lab test",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — DOCTOR role required"),
            @ApiResponse(responseCode = "404", description = "Patient or lab test not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabOrderResponse create(
            @Valid @RequestBody LabOrderRequest request,
            Authentication authentication
    ) {

        return labOrderService.create(
                request,
                authentication.getName()
        );
    }

    @Operation(summary = "Get a lab order by ID",
            description = "Returns a lab order only if it belongs to the authenticated doctor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lab order found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — order belongs to a different doctor"),
            @ApiResponse(responseCode = "404", description = "Lab order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public LabOrderResponse getById(
            @Parameter(description = "Lab order ID") @PathVariable Long id,
            Authentication authentication
    ) {

        return labOrderService.getForDoctor(
                id,
                authentication.getName()
        );
    }

    @Operation(
            summary = "Update lab order status",
            description = "Transitions a lab order to a new status " +
                          "(e.g., PENDING → IN_PROGRESS → COMPLETED). " +
                          "Only the owning doctor can update the status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — order belongs to a different doctor"),
            @ApiResponse(responseCode = "404", description = "Lab order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public LabOrderResponse updateStatus(
            @Parameter(description = "Lab order ID") @PathVariable Long id,
            @Parameter(description = "New status value") @RequestParam LabOrderStatus status,
            Authentication authentication
    ) {

        return labOrderService.updateStatus(
                id,
                status,
                authentication.getName()
        );
    }
}