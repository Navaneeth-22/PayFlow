package com.payflow.payment.messaging;

import com.payflow.payment.domain.service.SagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final SagaService sagaService;


    @KafkaListener(topics = "fraud.cleared", groupId = "payment-service-fraud-group")
    public void onFraudCleared(Map<String, Object> event) {
        try {
            log.info("FRAUD_CLEARED received: paymentId={}", event.get("paymentId"));
            sagaService.handleFraudCleared(event);
        } catch (Exception e) {

            log.error("Failed to process FRAUD_CLEARED event={} error={}",
                    event, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "fraud.flagged", groupId = "payment-service-fraud-group")
    public void onFraudFlagged(Map<String, Object> event) {
        try {
            log.info("FRAUD_FLAGGED received: paymentId={} reason={}",
                    event.get("paymentId"), event.get("reason"));
            sagaService.handleFraudFlagged(event);
        } catch (Exception e) {
            log.error("Failed to process FRAUD_FLAGGED event={} error={}",
                    event, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ledger.debited", groupId = "payment-service-ledger-group")
    public void onLedgerDebited(Map<String, Object> event) {
        try {
            log.info("LEDGER_DEBITED received: paymentId={}", event.get("paymentId"));
            sagaService.handleLedgerDebited(event);
        } catch (Exception e) {
            log.error("Failed to process LEDGER_DEBITED event={} error={}",
                    event, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ledger.failed", groupId = "payment-service-ledger-group")
    public void onLedgerFailed(Map<String, Object> event) {
        try {
            log.info("LEDGER_FAILED received: paymentId={} reason={}",
                    event.get("paymentId"), event.get("reason"));
            sagaService.handleLedgerFailed(event);
        } catch (Exception e) {
            log.error("Failed to process LEDGER_FAILED event={} error={}",
                    event, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ledger.reversed", groupId = "payment-service-ledger-group")
    public void onLedgerReversed(Map<String, Object> event) {
        try {
            log.info("LEDGER_REVERSED received: paymentId={}", event.get("paymentId"));
            sagaService.handleLedgerReversed(event);
        } catch (Exception e) {
            log.error("Failed to process LEDGER_REVERSED: paymentId={} error={}",
                    event.get("paymentId"), e.getMessage(), e);
        }
    }
}