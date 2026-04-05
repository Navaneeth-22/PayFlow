package com.payflow.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemAccount {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}