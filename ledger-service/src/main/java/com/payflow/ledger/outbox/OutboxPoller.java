package com.payflow.ledger.outbox;

import com.payflow.ledger.domain.model.OutboxEvent;
import com.payflow.ledger.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxPublisher outboxPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String instanceId = UUID.randomUUID().toString();
    private static final Duration CLAIM_TTL = Duration.ofSeconds(30);

    @Scheduled(fixedDelay = 500)
    public void publishPendingEvents() {

        Instant expiry = Instant.now().minus(CLAIM_TTL);
        int claimed = outboxPublisher.claimRows(
                instanceId, Instant.now(), expiry, 100
        );
        if (claimed == 0) return;

        List<OutboxEvent> mine = outboxPublisher.fetchClaimedBy(instanceId);

        List<UUID> successIds = new ArrayList<>();
        for (var event : mine) {
            try {
                String topic = event.getEventType()
                        .toLowerCase().replace("_", ".");
                String partitionKey = resolvePartitionKey(event);

                kafkaTemplate.send(topic, partitionKey, event.getPayload()).get();
                successIds.add(event.getId());

                log.debug("Published {} for aggregateId: {}",
                        event.getEventType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}",
                        event.getId(), e.getMessage());
            }
        }

        if (!successIds.isEmpty()) {
            outboxPublisher.markPublished(successIds, Instant.now());
        }
    }

    private String resolvePartitionKey(OutboxEvent event) {
        return event.getAggregateId().toString();
    }
}