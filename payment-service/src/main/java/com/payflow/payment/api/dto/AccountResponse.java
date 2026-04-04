package com.payflow.payment.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID id;
    private UUID userId;
    private String currency;
    private String status;
    private Instant createdAt;
}