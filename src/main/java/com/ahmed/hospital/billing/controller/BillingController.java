package com.ahmed.hospital.billing.controller;

import com.ahmed.hospital.billing.dto.CreateInvoiceRequest;
import com.ahmed.hospital.billing.dto.InvoiceResponse;
import com.ahmed.hospital.billing.service.BillingService;
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
@RequiredArgsConstructor
@RequestMapping("/api/receptionist/invoices")
@PreAuthorize("hasRole('RECEPTIONIST')")
@Tag(name = "Receptionist — Billing",
        description = "Invoice creation and retrieval. Requires RECEPTIONIST role.")
public class BillingController {

    private final BillingService billingService;

    @Operation(summary = "Create an invoice",
            description = "Generates an invoice for a patient visit or service. " +
                          "The invoice starts in an UNPAID state.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invoice created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — RECEPTIONIST role required")
    })
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request
    ) {

        InvoiceResponse response =
                billingService.createInvoice(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get invoice by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @Parameter(description = "Invoice ID") @PathVariable Long invoiceId
    ) {

        return ResponseEntity.ok(
                billingService.getInvoice(invoiceId)
        );
    }

    @Operation(summary = "List all invoices for a patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<InvoiceResponse>> getPatientInvoices(
            @Parameter(description = "Patient ID") @PathVariable Long patientId
    ) {

        return ResponseEntity.ok(
                billingService.getPatientInvoices(patientId)
        );
    }
}