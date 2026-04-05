package com.payflow.ledger.messaging;

import com.payflow.ledger.domain.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventConsumer {

    private final LedgerService ledgerService;

    @KafkaListener(
            topics = "fraud.cleared",
            groupId = "ledger-service-fraud-group"
    )
    public void onFraudCleared(Map<String, Object> event) {
        try{
            log.info("FRAUD_CLEARED received by ledger: paymentId={}",
                    event.get("paymentId"));
            ledgerService.processPayment(event);
        }catch (Exception e) {
            log.error("Unexpected failure in ledger processing paymentId={} error={}",
                    event.get("paymentId"), e.getMessage(), e);
        }
    }
}