package com.payflow.ledger.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StatementResponse {
    private UUID accountId;
    private String currency;
    private BigDecimal currentBalance;
    private List<StatementLineResponse> entries;
    private int page;
    private int totalPages;
    private long totalEntries;
}
