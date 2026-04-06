package com.payflow.notification.domain.service;

import com.payflow.notification.domain.model.Webhook;
import com.payflow.notification.domain.model.WebhookDelivery;
import com.payflow.notification.domain.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSigningService signingService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${webhook.max-attempts:5}")
    private int maxAttempts;

    @Value("${webhook.retry-delays-seconds:30,120,480,1800,7200}")
    private String retryDelaysConfig;

    @Transactional
    public void deliver(WebhookDelivery delivery, Webhook webhook) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        try {
            String payloadJson = objectMapper.writeValueAsString(
                    delivery.getPayload()
            );
            String signature = signingService.sign(payloadJson, webhook.getSecret());
            var response = restClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-PayFlow-Signature", signature)
                    .header("X-PayFlow-Event",     delivery.getEventType())
                    .header("X-PayFlow-Delivery",  delivery.getId().toString())
                    .header("X-PayFlow-Timestamp", Instant.now().toString())
                    .body(payloadJson)
                    .retrieve()
                    .toEntity(String.class);

            int httpStatus = response.getStatusCode().value();
            delivery.setLastHttpStatus(httpStatus);

            String responseBody = response.getBody();
            if (responseBody != null) {
                delivery.setLastResponse(
                        responseBody.substring(0, Math.min(500, responseBody.length()))
                );
            }

            if (response.getStatusCode().is2xxSuccessful()) {
                delivery.setStatus("DELIVERED");
                log.info("Webhook DELIVERED: deliveryId={} attempt={}",
                        delivery.getId(), delivery.getAttemptCount());
            } else {
                log.warn("Webhook delivery failed with HTTP {}: deliveryId={} attempt={}",
                        httpStatus, delivery.getId(), delivery.getAttemptCount());
                scheduleRetry(delivery);
            }

        } catch (Exception e) {
            log.warn("Webhook delivery error: deliveryId={} attempt={} error={}",
                    delivery.getId(), delivery.getAttemptCount(), e.getMessage());
            delivery.setLastResponse("Network error: " + e.getMessage());
            scheduleRetry(delivery);
        }

        deliveryRepository.save(delivery);
    }

    private void scheduleRetry(WebhookDelivery delivery) {
        if (delivery.getAttemptCount() >= maxAttempts) {
            delivery.setStatus("EXHAUSTED");
            log.warn("Webhook EXHAUSTED after {} attempts: deliveryId={}",
                    maxAttempts, delivery.getId());
            return;
        }
        List<Long> delays = Arrays.stream(retryDelaysConfig.split(","))
                .map(s -> Long.parseLong(s.trim()))
                .toList();

        int delayIndex = Math.min(
                delivery.getAttemptCount() - 1,
                delays.size() - 1
        );
        long delaySeconds = delays.get(delayIndex);

        delivery.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
        delivery.setStatus("PENDING");

        log.info("Webhook retry scheduled: deliveryId={} nextAttemptIn={}s attempt={}",
                delivery.getId(), delaySeconds, delivery.getAttemptCount());
    }
}