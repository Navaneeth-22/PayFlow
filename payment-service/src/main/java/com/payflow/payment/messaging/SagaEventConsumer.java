package com.payflow.payment.messaging;

import com.payflow.payment.domain.model.*;
import com.payflow.payment.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final SagaStateRepository sagaStateRepository;
    private final PaymentRepository paymentRepository;

    @KafkaListener(topics = "fraud.cleared", groupId = "payment-service-fraud-group")
    @Transactional
    public void onFraudCleared(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        log.info("FRAUD_CLEARED received for paymentId: {}", paymentId);
        SagaState saga = sagaStateRepository.findById(paymentId).orElse(null);
        if (saga == null || saga.getStatus() != SagaStatus.FRAUD_CHECK) {
            log.warn("Skipping FRAUD_CLEARED — saga not in FRAUD_CHECK state. paymentId={}", paymentId);
            return;
        }


        saga.setStatus(SagaStatus.DEBITING);
        saga.setLastEvent("FRAUD_CLEARED");
        sagaStateRepository.save(saga);

        log.info("Saga advanced: FRAUD_CHECK → DEBITING for paymentId: {}", paymentId);
    }

    @KafkaListener(topics = "fraud.flagged", groupId = "payment-service-fraud-group")
    @Transactional
    public void onFraudFlagged(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        String reason = (String) event.get("reason");
        log.info("FRAUD_FLAGGED received for paymentId: {} reason: {}", paymentId, reason);

        SagaState saga = sagaStateRepository.findById(paymentId).orElse(null);
        if (saga == null || saga.getStatus() != SagaStatus.FRAUD_CHECK) {
            log.warn("Skipping FRAUD_FLAGGED — saga not in FRAUD_CHECK state. paymentId={}", paymentId);
            return;
        }
        saga.setStatus(SagaStatus.FAILED);
        saga.setLastEvent("FRAUD_FLAGGED");
        saga.setFailureReason(reason);
        sagaStateRepository.save(saga);
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Fraud check failed: " + reason);
            paymentRepository.save(payment);
        });

        log.info("Saga FAILED at fraud check for paymentId: {} reason: {}", paymentId, reason);
    }
}