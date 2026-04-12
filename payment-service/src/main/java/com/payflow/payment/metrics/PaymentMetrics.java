package com.payflow.payment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentMetrics {

    private final Counter paymentsInitiated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;
    private final Counter sagasStuckDetected;
    private final Counter idempotencyHits;
    private final Counter idempotencyMismatches;

    public PaymentMetrics(MeterRegistry registry) {
        this.paymentsInitiated = Counter.builder("payments.initiated")
                .description("Total number of payments initiated")
                .register(registry);

        this.paymentsCompleted = Counter.builder("payments.completed")
                .description("Total payments that reached COMPLETED")
                .register(registry);

        this.paymentsFailed = Counter.builder("payments.failed")
                .description("Total failed payments")
                .register(registry);

        this.sagasStuckDetected = Counter.builder("saga.stuck.detected")
                .description("Number of sagas force-failed by stuck detector")
                .register(registry);

        this.idempotencyHits = Counter.builder("idempotency.hits")
                .description("Idempotency key cache hits")
                .register(registry);

        this.idempotencyMismatches = Counter.builder("idempotency.mismatches")
                .description("Idempotency key reused with different body")
                .register(registry);
    }

    public void paymentInitiated() { paymentsInitiated.increment(); }
    public void paymentCompleted() { paymentsCompleted.increment(); }
    public void paymentFailed(String reason) { paymentsFailed.increment(); }
    public void sagaStuckDetected() { sagasStuckDetected.increment(); }
    public void idempotencyHit() { idempotencyHits.increment(); }
    public void idempotencyMismatch() { idempotencyMismatches.increment(); }
}