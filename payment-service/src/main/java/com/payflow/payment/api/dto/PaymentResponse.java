package com.payflow.payment.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private UUID fromAccountId;
    private UUID toAccountId;
    private String failureReason;
    private Instant createdAt;
}