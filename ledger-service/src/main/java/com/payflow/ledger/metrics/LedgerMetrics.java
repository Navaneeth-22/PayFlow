package com.payflow.ledger.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LedgerMetrics {

    private final Counter entriesCreated;
    private final Counter paymentsProcessed;
    private final Counter paymentsFailed;
    private final Counter reversalsProcessed;

    public LedgerMetrics(MeterRegistry registry) {

        this.entriesCreated = Counter.builder("ledger.entries.created")
                .description("Total journal entries created")
                .register(registry);

        this.paymentsProcessed = Counter.builder("ledger.payments.processed")
                .description("Total payments processed by ledger")
                .register(registry);

        this.paymentsFailed = Counter.builder("ledger.payments.failed")
                .description("Payments rejected by ledger (insufficient funds etc)")
                .register(registry);

        this.reversalsProcessed = Counter.builder("ledger.reversals.processed")
                .description("Total payment reversals processed")
                .register(registry);
    }

    public void entriesCreated(int count) { entriesCreated.increment(count); }
    public void paymentProcessed() { paymentsProcessed.increment(); }
    public void paymentFailed() { paymentsFailed.increment(); }
    public void reversalProcessed() { reversalsProcessed.increment(); }
}