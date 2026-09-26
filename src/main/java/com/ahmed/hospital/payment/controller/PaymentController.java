package com.ahmed.hospital.payment.controller;

import com.ahmed.hospital.common.response.ErrorResponse;
import com.ahmed.hospital.payment.dto.CreatePaymentRequest;
import com.ahmed.hospital.payment.dto.PaymentResponse;
import com.ahmed.hospital.payment.service.PaymentService;
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
@RequestMapping("/api/receptionist/payments")
@PreAuthorize("hasRole('RECEPTIONIST')")
@Tag(name = "Receptionist — Payments",
        description = "Idempotent payment recording against invoices. Requires RECEPTIONIST role.")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Record a payment",
            description = "Records a payment against an invoice. " +
                          "This endpoint is **idempotent**: provide a unique `Idempotency-Key` header " +
                          "per transaction attempt. Repeating the same key returns the original response " +
                          "without creating a duplicate payment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment recorded"),
            @ApiResponse(responseCode = "400", description = "Validation error or invoice already fully paid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — RECEPTIONIST role required"),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(description = "Unique key to guarantee idempotency (UUID recommended)",
                    required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        paymentService.createPayment(
                                request,
                                idempotencyKey
                        )
                );
    }

    @Operation(summary = "Get payment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID") @PathVariable Long paymentId
    ) {

        return ResponseEntity.ok(
                paymentService.getPayment(paymentId)
        );
    }

    @Operation(summary = "List all payments for an invoice")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentResponse>> getInvoicePayments(
            @Parameter(description = "Invoice ID") @PathVariable Long invoiceId
    ) {

        return ResponseEntity.ok(
                paymentService.getInvoicePayments(invoiceId)
        );
    }
}