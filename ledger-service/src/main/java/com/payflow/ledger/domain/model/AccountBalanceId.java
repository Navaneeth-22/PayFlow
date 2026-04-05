package com.payflow.ledger.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AccountBalanceId implements Serializable {

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "currency")
    private String currency;
}