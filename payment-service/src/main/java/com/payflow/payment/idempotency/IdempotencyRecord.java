package com.payflow.payment.idempotency;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "idempotency_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyRecord {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @Column(name = "response_status")
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body")
    private Map<String, Object> responseBody;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}