// PaymentInitiatedEvent.java
package com.payflow.fraud.messaging.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentInitiatedEvent {
    private UUID paymentId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID userId;           // who initiated the payment
    private BigDecimal amount;
    private String currency;
}