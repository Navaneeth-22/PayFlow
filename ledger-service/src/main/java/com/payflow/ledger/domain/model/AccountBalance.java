package com.payflow.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "account_balances")
@Getter
@NoArgsConstructor
public class AccountBalance {

    @EmbeddedId
    private AccountBalanceId id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "total_entries")
    private Long totalEntries;

    @Column(name = "last_entry_at")
    private Instant lastEntryAt;
}