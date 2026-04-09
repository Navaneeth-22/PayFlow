package com.payflow.payment.outbox;

import com.payflow.payment.domain.model.OutboxEvent;
import com.payflow.payment.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public int claimRows(String instanceId, Instant now,
                         Instant expiry, int limit) {
        return outboxEventRepository.claimRows(
                instanceId, now, expiry, limit
        );
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> fetchClaimedBy(String instanceId) {
        return outboxEventRepository.findClaimedBy(instanceId);
    }

    @Transactional
    public void markPublished(List<UUID> ids, Instant now) {
        outboxEventRepository.markPublishedByIds(ids, now);
    }
}