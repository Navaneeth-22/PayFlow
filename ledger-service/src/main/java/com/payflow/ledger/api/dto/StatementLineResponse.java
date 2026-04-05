package com.payflow.ledger.api.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class StatementLineResponse {
    private UUID paymentId;
    private String entryType;
    private BigDecimal amount;
    private String currency;
    private String narration;
    private BigDecimal runningBalance;
    private Instant createdAt;
}
