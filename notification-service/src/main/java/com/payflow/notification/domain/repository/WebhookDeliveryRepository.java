package com.payflow.notification.domain.repository;

import com.payflow.notification.domain.model.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository
        extends JpaRepository<WebhookDelivery, UUID> {

    boolean existsByWebhookIdAndEventId(UUID webhookId, UUID eventId);

    List<WebhookDelivery> findTop50ByStatusAndNextAttemptAtBefore(
            String status, Instant now
    );

    Page<WebhookDelivery> findByWebhookIdOrderByCreatedAtDesc(
            UUID webhookId, Pageable pageable
    );
}