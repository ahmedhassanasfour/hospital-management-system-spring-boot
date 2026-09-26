package com.ahmed.hospital.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnpaidInvoiceScheduler {

    @Scheduled(cron = "0 0 9 * * *")
    public void processUnpaidInvoices() {

        log.info(
                "Running unpaid invoice reminder job"
        );

        // Later:
        // Find overdue invoices
        // Create notification
        // Send email
    }
}