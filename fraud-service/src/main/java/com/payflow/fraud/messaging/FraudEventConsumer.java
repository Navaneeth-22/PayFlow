package com.payflow.fraud.messaging;

import com.payflow.fraud.domain.service.FraudEvaluationService;
import com.payflow.fraud.messaging.dto.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventConsumer {

    private final FraudEvaluationService fraudEvaluationService;

    @KafkaListener(
            topics = "payment.initiated",
            groupId = "fraud-service-group"
    )
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        try{
            log.info("Received PAYMENT_INITIATED: paymentId={} amount={}",
                    event.getPaymentId(), event.getAmount());
            fraudEvaluationService.evaluate(event);
        } catch (Exception e) {
            log.error("Failed to evaluate the PaymentId={} error={}",event.getPaymentId(),e.getMessage(),e);
        }
    }
}