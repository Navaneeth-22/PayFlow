package com.payflow.payment.domain.service;

import com.payflow.payment.domain.model.*;
import com.payflow.payment.domain.repository.*;
import com.payflow.payment.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaService {

    private final SagaStateRepository sagaStateRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;

    @Transactional
    public void handleFraudCleared(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));

        SagaState saga = sagaStateRepository.findById(paymentId)
                .orElse(null);

        if (saga == null) {
            log.warn("FRAUD_CLEARED: no saga found for paymentId={}", paymentId);
            return;
        }
        if (saga.getStatus() != SagaStatus.FRAUD_CHECK) {
            log.warn("FRAUD_CLEARED: wrong state. paymentId={} currentStatus={}",
                    paymentId, saga.getStatus());
            return;
        }

        saga.setStatus(SagaStatus.DEBITING);
        saga.setLastEvent("FRAUD_CLEARED");
        sagaStateRepository.save(saga);

        log.info("Saga FRAUD_CHECK → DEBITING for paymentId={}", paymentId);
    }

    @Transactional
    public void handleFraudFlagged(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        String reason  = (String) event.get("reason");

        SagaState saga = sagaStateRepository.findById(paymentId)
                .orElse(null);

        if (saga == null) {
            log.warn("FRAUD_FLAGGED: no saga found for paymentId={}", paymentId);
            return;
        }
        if (saga.getStatus() != SagaStatus.FRAUD_CHECK) {
            log.warn("FRAUD_FLAGGED: wrong state. paymentId={} currentStatus={}",
                    paymentId, saga.getStatus());
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

        outboxService.save(
                paymentId,
                "PAYMENT",
                "PAYMENT_FAILED",
                Map.of(
                        "paymentId",      paymentId.toString(),
                        "fromAccountId",  "",
                        "failureReason",  reason != null ? reason : "",
                        "status",         "FAILED"
                )
        );

        log.info("Saga FAILED (fraud) for paymentId={} reason={}", paymentId, reason);
    }


    @Transactional
    public void handleLedgerDebited(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));

        SagaState saga = sagaStateRepository.findById(paymentId)
                .orElse(null);

        if (saga == null) {
            log.warn("LEDGER_DEBITED: no saga found for paymentId={}", paymentId);
            return;
        }
        if (saga.getStatus() != SagaStatus.DEBITING) {
            log.warn("LEDGER_DEBITED: wrong state. paymentId={} currentStatus={}",
                    paymentId, saga.getStatus());
            return;
        }

        saga.setStatus(SagaStatus.COMPLETED);
        saga.setLastEvent("LEDGER_DEBITED");
        sagaStateRepository.save(saga);

        paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            outboxService.save(
                    paymentId,
                    "PAYMENT",
                    "PAYMENT_COMPLETED",
                    Map.of(
                            "paymentId",     paymentId.toString(),
                            "fromAccountId", payment.getFromAccountId().toString(),
                            "toAccountId",   payment.getToAccountId().toString(),
                            "amount",        payment.getAmount(),
                            "currency",      payment.getCurrency(),
                            "status",        "COMPLETED"
                    )
            );
        });


        log.info("Saga COMPLETED for paymentId={} — money has moved", paymentId);
    }

    @Transactional
    public void handleLedgerFailed(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        String reason  = (String) event.get("reason");

        SagaState saga = sagaStateRepository.findById(paymentId)
                .orElse(null);

        if (saga == null) {
            log.warn("LEDGER_FAILED: no saga found for paymentId={}", paymentId);
            return;
        }
        if (saga.getStatus() != SagaStatus.DEBITING) {
            log.warn("LEDGER_FAILED: wrong state. paymentId={} currentStatus={}",
                    paymentId, saga.getStatus());
            return;
        }

        saga.setStatus(SagaStatus.FAILED);
        saga.setLastEvent("LEDGER_FAILED");
        saga.setFailureReason(reason);
        sagaStateRepository.save(saga);

        paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
        });
        outboxService.save(
                paymentId,
                "PAYMENT",
                "PAYMENT_FAILED",
                Map.of(
                        "paymentId",      paymentId.toString(),
                        "fromAccountId",  "",
                        "failureReason",  reason != null ? reason : "",
                        "status",         "FAILED"
                )
        );

        log.info("Saga FAILED (ledger) for paymentId={} reason={}", paymentId, reason);
    }
}