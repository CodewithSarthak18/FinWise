package com.finwise.scheduler;

import com.finwise.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "finwise.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class RecurringPaymentScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringPaymentScheduler.class);
    private final RecurringTransactionService recurringTransactionService;

    /**
     * Executes daily at 02:00 AM UTC to reliably process all due recurring payments.
     * Prevents duplicate execution and handles insufficient funds safely.
     */
    @Scheduled(cron = "${finwise.scheduler.recurring-cron:0 0 2 * * ?}")
    public void runScheduledRecurringPayments() {
        log.info("Starting automated recurring payment processing job at {}", Instant.now());
        long startTime = System.currentTimeMillis();

        try {
            int processedCount = recurringTransactionService.processDueRecurringTransactions();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Finished recurring payment processing job in {} ms. Successfully processed {} transactions.",
                    duration, processedCount);
        } catch (Exception ex) {
            log.error("Unhandled error during scheduled recurring payment processing: {}", ex.getMessage(), ex);
        }
    }
}
