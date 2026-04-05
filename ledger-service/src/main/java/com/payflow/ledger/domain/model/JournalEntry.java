package com.payflow.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    @Column(name = "counterpart_id", nullable = false)
    private UUID counterpartId;
    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "entry_ref", nullable = false, unique = true)
    private String entryRef;

    @Column
    private String narration;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}