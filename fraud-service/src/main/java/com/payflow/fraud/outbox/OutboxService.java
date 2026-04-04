package com.payflow.fraud.outbox;

import com.payflow.fraud.domain.model.OutboxEvent;
import com.payflow.fraud.domain.repository.OutboxEventRepository;
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