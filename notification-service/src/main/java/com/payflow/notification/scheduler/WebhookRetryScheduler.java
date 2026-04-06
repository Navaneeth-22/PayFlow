package com.payflow.notification.scheduler;

import com.payflow.notification.domain.model.Webhook;
import com.payflow.notification.domain.model.WebhookDelivery;
import com.payflow.notification.domain.repository.WebhookDeliveryRepository;
import com.payflow.notification.domain.repository.WebhookRepository;
import com.payflow.notification.domain.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryScheduler {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryService deliveryService;

    @Scheduled(fixedDelay = 30_000)
    public void processDeliveries() {
        List<WebhookDelivery> due = deliveryRepository
                .findTop50ByStatusAndNextAttemptAtBefore("PENDING", Instant.now());

        if (due.isEmpty()) {
            log.debug("No webhook deliveries due");
            return;
        }

        log.info("Processing {} pending webhook deliveries", due.size());

        for (WebhookDelivery delivery : due) {
            try {
                Webhook webhook = webhookRepository
                        .findById(delivery.getWebhookId())
                        .orElse(null);

                if (webhook == null) {
                    log.warn("Webhook not found for delivery: deliveryId={}",
                            delivery.getId());
                    delivery.setStatus("EXHAUSTED");
                    delivery.setLastResponse("Webhook deleted");
                    deliveryRepository.save(delivery);
                    continue;
                }

                if (!webhook.getActive()) {
                    log.warn("Webhook deactivated: webhookId={} deliveryId={}",
                            webhook.getId(), delivery.getId());
                    delivery.setStatus("EXHAUSTED");
                    delivery.setLastResponse("Webhook deactivated");
                    deliveryRepository.save(delivery);
                    continue;
                }

                deliveryService.deliver(delivery, webhook);

            } catch (Exception e) {
                log.error("Unexpected scheduler error for deliveryId={}: {}",
                        delivery.getId(), e.getMessage(), e);
            }
        }
    }
}