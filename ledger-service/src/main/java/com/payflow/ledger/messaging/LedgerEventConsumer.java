package com.payflow.ledger.messaging;

import com.payflow.ledger.domain.model.CancelledPayment;
import com.payflow.ledger.domain.repository.CancelledPaymentRepository;
import com.payflow.ledger.domain.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventConsumer {

    private final LedgerService ledgerService;
    private final CancelledPaymentRepository cancelledPaymentRepository;

    @KafkaListener(
            topics = "fraud.cleared",
            groupId = "ledger-service-fraud-group"
    )
    public void onFraudCleared(Map<String, Object> event) {
            log.info("FRAUD_CLEARED received by ledger: paymentId={}",
                    event.get("paymentId"));
            ledgerService.processPayment(event);
    }

    @KafkaListener(
            topics = "payment.cancelled",
            groupId = "ledger-payment-cancelled-group"
    )
    public void onPaymentCancelled(Map<String, Object> event) {
            log.info("PAYMENT_CANCELLED received by ledger: paymentId={}", event.get("paymentId"));
            ledgerService.cancelPayment(event);
    }

    @KafkaListener(
            topics = "payment.reversal.needed",
            groupId = "ledger-reversal-group"
    )
    public void onPaymentReversalNeeded(Map<String, Object> event) {
            log.info("PAYMENT_REVERSAL_NEEDED received: paymentId={}",
                    event.get("paymentId"));
            ledgerService.reversePayment(event);
    }
}