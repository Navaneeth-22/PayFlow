package com.payflow.notification.domain.repository;

import com.payflow.notification.domain.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    @Query(
            value = "SELECT * FROM webhooks " +
                    "WHERE merchant_id = :merchantId " +
                    "AND active = TRUE " +
                    "AND events @> ARRAY[:eventType]::TEXT[]",
            nativeQuery = true
    )
    List<Webhook> findActiveByMerchantIdAndEventType(
            @Param("merchantId") UUID merchantId,
            @Param("eventType") String eventType
    );

    List<Webhook> findByMerchantIdAndActiveTrue(UUID merchantId);
}