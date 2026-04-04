package com.payflow.payment.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    void deleteExpired(Instant now);
}