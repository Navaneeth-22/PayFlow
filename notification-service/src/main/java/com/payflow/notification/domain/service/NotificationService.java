package com.payflow.notification.domain.service;

import com.payflow.notification.domain.model.Webhook;
import com.payflow.notification.domain.model.WebhookDelivery;
import com.payflow.notification.domain.repository.WebhookDeliveryRepository;
import com.payflow.notification.domain.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    @Transactional
    public void handlePaymentEvent(Map<String, Object> event,
                                   String eventType) {

        UUID paymentId = UUID.fromString((String) event.get("paymentId"));

        String fromAccountStr = (String) event.get("fromAccountId");
        if (fromAccountStr == null) {
            log.warn("No fromAccountId in event — cannot find webhooks. " +
                    "paymentId={}", paymentId);
            return;
        }
        UUID merchantId = UUID.fromString(fromAccountStr);

        List<Webhook> webhooks = webhookRepository.findActiveByMerchantIdAndEventType(merchantId, eventType);

        if (webhooks.isEmpty()) {
            log.debug("No webhooks for merchantId={} eventType={}",
                    merchantId, eventType);
            return;
        }

        Map<String, Object> payload = Map.of(
                "eventType",  eventType,
                "paymentId",  paymentId.toString(),
                "amount",     event.getOrDefault("amount", ""),
                "currency",   event.getOrDefault("currency", ""),
                "occurredAt", Instant.now().toString()
        );

        for (Webhook webhook : webhooks) {
            if (deliveryRepository.existsByWebhookIdAndEventId(webhook.getId(), paymentId)) {
                log.debug("Skipping duplicate. webhookId={} paymentId={}",
                        webhook.getId(), paymentId);
                continue;
            }

            WebhookDelivery delivery = WebhookDelivery.builder()
                    .webhookId(webhook.getId())
                    .eventId(paymentId)
                    .eventType(eventType)
                    .payload(payload)
                    .nextAttemptAt(Instant.now())
                    .build();

            deliveryRepository.save(delivery);

            log.info("Webhook delivery queued: webhookId={} eventType={} paymentId={}",
                    webhook.getId(), eventType, paymentId);
        }
    }
}