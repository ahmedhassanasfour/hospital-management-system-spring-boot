package com.ahmed.hospital.payment.service;

import com.ahmed.hospital.audit.service.AuditService;
import com.ahmed.hospital.billing.entity.Invoice;
import com.ahmed.hospital.billing.entity.InvoiceStatus;
import com.ahmed.hospital.billing.repository.InvoiceRepository;
import com.ahmed.hospital.common.event.NotificationEvent;
import com.ahmed.hospital.notification.entity.NotificationType;
import com.ahmed.hospital.payment.dto.CreatePaymentRequest;
import com.ahmed.hospital.payment.dto.PaymentResponse;
import com.ahmed.hospital.payment.entity.IdempotencyRecord;
import com.ahmed.hospital.payment.entity.Payment;
import com.ahmed.hospital.payment.entity.PaymentStatus;
import com.ahmed.hospital.payment.repository.IdempotencyRecordRepository;
import com.ahmed.hospital.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;


    public PaymentResponse createPayment(
            CreatePaymentRequest request,
            String idempotencyKey
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "Idempotency-Key header is required"
            );
        }

        Optional<IdempotencyRecord> existingRecord =
                idempotencyRecordRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {
            // ── DUPLICATE REQUEST: return the original response, no new notification ─
            log.debug(
                    "Idempotent replay for key={} — returning original payment {}",
                    idempotencyKey,
                    existingRecord.get().getPaymentId()
            );

            Payment existingPayment =
                    paymentRepository.findById(
                            existingRecord.get().getPaymentId()
                    ).orElseThrow(() ->
                            new com.ahmed.hospital.common.exception.ResourceNotFoundException(
                                    "Original payment not found"
                            )
                    );

            return toResponse(existingPayment);
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() ->
                        new com.ahmed.hospital.common.exception.ResourceNotFoundException("Invoice not found")
                );

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "Cannot pay a cancelled invoice"
            );
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "Invoice is already fully paid"
            );
        }

        BigDecimal totalPaid =
                paymentRepository.calculateTotalPaid(
                        invoice.getId(),
                        PaymentStatus.COMPLETED
                );

        BigDecimal remaining =
                invoice.getTotalAmount()
                        .subtract(totalPaid);

        if (request.amount().compareTo(remaining) > 0) {
            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "Payment amount exceeds remaining invoice amount"
            );
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .paymentReference(generatePaymentReference())
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .notes(request.notes())
                .build();

        payment.markCompleted();

        Payment savedPayment =
                paymentRepository.save(payment);

        IdempotencyRecord record =
                IdempotencyRecord.builder()
                        .idempotencyKey(idempotencyKey)
                        .paymentId(savedPayment.getId())
                        .build();

        idempotencyRecordRepository.save(record);

        updateInvoiceStatus(invoice);

        // ── Audit: new payment recorded ───────────────────────────────────────────
        Long patientUserId = invoice.getPatient().getUser().getId();

        auditService.log(
                patientUserId,
                "PAYMENT_CREATED",
                "Payment",
                savedPayment.getId(),
                "Payment of " + savedPayment.getAmount() + " recorded for invoice " + invoice.getId(),
                null
        );

        // ── Event: fires after this transaction commits ──────────────────────────
        // The idempotency check above prevents a second event for replay requests.
        String patientName  = invoice.getPatient().getUser().getFirstName()
                + " " + invoice.getPatient().getUser().getLastName();
        String patientEmail = invoice.getPatient().getUser().getEmail();

        if (savedPayment.getStatus() == PaymentStatus.COMPLETED) {
            eventPublisher.publishEvent(
                    new NotificationEvent(
                            patientUserId,
                            NotificationType.PAYMENT_SUCCESS,
                            "Payment Received",
                            "Your payment of " + savedPayment.getAmount()
                                    + " for invoice #" + invoice.getInvoiceNumber()
                                    + " has been successfully processed.",
                            patientEmail,
                            patientName,
                            savedPayment.getId()
                    )
            );
        } else if (savedPayment.getStatus() == PaymentStatus.FAILED) {
            eventPublisher.publishEvent(
                    new NotificationEvent(
                            patientUserId,
                            NotificationType.PAYMENT_FAILED,
                            "Payment Failed",
                            "Your payment of " + savedPayment.getAmount()
                                    + " for invoice #" + invoice.getInvoiceNumber()
                                    + " could not be processed.",
                            patientEmail,
                            patientName,
                            savedPayment.getId()
                    )
            );
        }

        return toResponse(savedPayment);
    }

    private void updateInvoiceStatus(Invoice invoice) {

        BigDecimal totalPaid =
                paymentRepository.calculateTotalPaid(
                        invoice.getId(),
                        PaymentStatus.COMPLETED
                );

        int comparison =
                totalPaid.compareTo(invoice.getTotalAmount());

        if (comparison >= 0) {

            invoice.setStatus(InvoiceStatus.PAID);

        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {

            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);

        } else {

            invoice.setStatus(InvoiceStatus.ISSUED);
        }

        invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getInvoicePayments(
            Long invoiceId
    ) {

        return paymentRepository
                .findByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new com.ahmed.hospital.common.exception.ResourceNotFoundException("Payment not found")
                );

        return toResponse(payment);
    }

    private String generatePaymentReference() {

        return "PAY-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getInvoice().getId(),
                payment.getPaymentReference(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getNotes()
        );
    }
}