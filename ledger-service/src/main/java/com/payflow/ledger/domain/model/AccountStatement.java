package com.payflow.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "account_statements")
@Getter
@NoArgsConstructor
public class AccountStatement {

    @EmbeddedId
    private AccountStatementId id;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column
    private String narration;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "running_balance", precision = 19, scale = 4)
    private BigDecimal runningBalance;
}