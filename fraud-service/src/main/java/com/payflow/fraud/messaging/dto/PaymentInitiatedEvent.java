package com.payflow.fraud.messaging.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentInitiatedEvent {
    private UUID paymentId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
}