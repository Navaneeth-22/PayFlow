package com.payflow.payment.outbox;

import com.payflow.payment.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        var unpublished = outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc();

        for (var event : unpublished) {
            try {
                String topic = event.getEventType()
                        .toLowerCase()
                        .replace("_", ".");

                String partitionKey = event.getPayload().containsKey("fromAccountId")
                        ? (String) event.getPayload().get("fromAccountId")
                        : event.getAggregateId().toString();

                kafkaTemplate.send(
                        topic,
                        partitionKey,
                        event.getPayload()
                ).get();

                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

                log.debug("Published {} to topic '{}' for aggregateId: {}",
                        event.getEventType(), topic, event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}",
                        event.getId(), e.getMessage());
            }
        }
    }
}