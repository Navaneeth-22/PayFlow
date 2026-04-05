package com.payflow.ledger.outbox;

import com.payflow.ledger.domain.model.OutboxEvent;
import com.payflow.ledger.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    public void save(UUID aggregateId, String aggregateType,
                     String eventType, Map<String, Object> payload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(payload)
                .build();
        outboxEventRepository.save(event);
    }
}