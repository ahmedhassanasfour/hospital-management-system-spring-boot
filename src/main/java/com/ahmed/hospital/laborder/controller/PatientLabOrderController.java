package com.ahmed.hospital.laborder.controller;

import com.ahmed.hospital.laborder.dto.LabOrderResponse;
import com.ahmed.hospital.laborder.service.LabOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient/lab-orders")
@RequiredArgsConstructor
@Tag(name = "Patient — Lab Orders",
        description = "Lab order history for the authenticated patient. Requires PATIENT role.")
public class PatientLabOrderController {

    private final LabOrderService labOrderService;

    @Operation(summary = "Get my lab order history",
            description = "Returns all lab orders associated with the authenticated patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lab order history returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — PATIENT role required")
    })
    @GetMapping
    public List<LabOrderResponse> getMyHistory(
            Authentication authentication
    ) {

        return labOrderService.getMyHistory(
                authentication.getName()
        );
    }
}