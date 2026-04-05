package com.payflow.ledger.api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BalanceResponse {
    private UUID accountId;
    private String currency;
    private BigDecimal balance;
    private Long totalEntries;
    private Instant lastEntryAt;
}