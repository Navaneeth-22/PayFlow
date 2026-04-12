package com.payflow.fraud.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FraudMetrics {

    private final Counter checksTotal;
    private final Counter flaggedTotal;
    private final Counter clearedTotal;
    private final Timer checkLatency;

    public FraudMetrics(MeterRegistry registry) {
        this.checksTotal = Counter.builder("fraud.checks.total")
                .description("Total fraud evaluations performed")
                .register(registry);

        this.flaggedTotal = Counter.builder("fraud.flagged.total")
                .description("Total payments flagged as fraudulent")
                .register(registry);

        this.clearedTotal = Counter.builder("fraud.cleared.total")
                .description("Total payments cleared by fraud check")
                .register(registry);

        this.checkLatency = Timer.builder("fraud.check.latency")
                .description("Time taken to evaluate fraud rules")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void checkPerformed() { checksTotal.increment(); }
    public void paymentFlagged() { flaggedTotal.increment(); }
    public void paymentCleared() { clearedTotal.increment(); }

    public void recordLatency(long milliseconds) {
        checkLatency.record(Duration.ofMillis(milliseconds));
    }
}