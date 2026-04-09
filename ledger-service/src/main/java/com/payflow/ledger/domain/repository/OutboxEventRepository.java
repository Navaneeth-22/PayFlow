package com.payflow.ledger.domain.repository;

import com.payflow.ledger.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Modifying
    @Query(value = """
    UPDATE outbox_events
    SET claimed_at = :now,
        claimed_by = :instanceId
    WHERE id IN (
        SELECT id FROM outbox_events
        WHERE published = false
        AND (claimed_at IS NULL OR claimed_at < :expiry)
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
    )
    """, nativeQuery = true)
    int claimRows(
            @Param("instanceId") String instanceId,
            @Param("now") Instant now,
            @Param("expiry") Instant expiry,
            @Param("limit") int limit
    );

    @Query("SELECT o FROM OutboxEvent o WHERE o.claimedBy = :instanceId AND o.published = false")
    List<OutboxEvent> findClaimedBy(@Param("instanceId") String instanceId);

    @Modifying
    @Query("""
    UPDATE OutboxEvent o
    SET o.published = true,
        o.publishedAt = :now,
        o.claimedAt = null,
        o.claimedBy = null
    WHERE o.id IN :ids
    """)
    void markPublishedByIds(@Param("ids") List<UUID> ids,
                            @Param("now") Instant now);
}