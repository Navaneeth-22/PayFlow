package com.payflow.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final Counter webhooksDelivered;
    private final Counter webhooksFailed;
    private final Counter webhooksExhausted;

    public NotificationMetrics(MeterRegistry registry) {
        this.webhooksDelivered = Counter.builder("webhooks.delivered")
                .description("Successfully delivered webhooks")
                .register(registry);

        this.webhooksFailed = Counter.builder("webhooks.failed")
                .description("Webhook delivery attempts that failed")
                .register(registry);

        this.webhooksExhausted = Counter.builder("webhooks.exhausted")
                .description("Webhooks that exhausted all retry attempts")
                .register(registry);
    }

    public void delivered() { webhooksDelivered.increment(); }
    public void failed() { webhooksFailed.increment(); }
    public void exhausted() { webhooksExhausted.increment(); }
}