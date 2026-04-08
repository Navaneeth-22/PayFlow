package com.payflow.payment.saga;

import com.payflow.payment.domain.model.*;
import com.payflow.payment.domain.repository.*;
import com.payflow.payment.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StuckSagaDetector {

    private final SagaStateRepository sagaStateRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;

    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(5);
    private static final List<SagaStatus> WAITING_STATES = List.of(
            SagaStatus.FRAUD_CHECK,
            SagaStatus.DEBITING
    );

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void detectAndFailStuckSagas() {
        Instant threshold = Instant.now().minus(STUCK_THRESHOLD);


        List<SagaState> stuckSagas = sagaStateRepository
                .findByStatusInAndUpdatedAtBefore(WAITING_STATES, threshold);

        if (stuckSagas.isEmpty()) {
            log.debug("Stuck saga detector: no stuck sagas found");
            return;
        }

        log.warn("Stuck saga detector: found {} stuck saga(s)", stuckSagas.size());

        for (SagaState saga : stuckSagas) {
            handleStuckSaga(saga);
        }
    }

    private void handleStuckSaga(SagaState saga) {
        log.warn("Stuck saga: paymentId={} status={} stuckSince={}",
                saga.getPaymentId(), saga.getStatus(), saga.getUpdatedAt());

        String reason = String.format(
                "Saga timed out in %s state after %d minutes.",
                saga.getStatus().name(),
                STUCK_THRESHOLD.toMinutes()
        );

        if (saga.getStatus() == SagaStatus.FRAUD_CHECK) {
            saga.setStatus(SagaStatus.FAILED);
            saga.setLastEvent("STUCK_SAGA_TIMEOUT");
            saga.setFailureReason(reason);
            sagaStateRepository.save(saga);

            paymentRepository.findById(saga.getPaymentId()).ifPresent(p -> {
                p.setStatus(PaymentStatus.FAILED);
                p.setFailureReason(reason);
                paymentRepository.save(p);
            });

            outboxService.save(saga.getPaymentId(), "PAYMENT", "PAYMENT_CANCELLED",
                    Map.of(
                            "paymentId", saga.getPaymentId().toString(),
                            "reason",    reason
                    )
            );

            log.warn("Stuck FRAUD_CHECK → FAILED. paymentId={}", saga.getPaymentId());
        }

        else if (saga.getStatus() == SagaStatus.DEBITING) {
            saga.setStatus(SagaStatus.REVERSAL_NEEDED);
            saga.setLastEvent("STUCK_SAGA_TIMEOUT");
            saga.setFailureReason(reason);
            sagaStateRepository.save(saga);

            paymentRepository.findById(saga.getPaymentId()).ifPresent(p -> {
                p.setStatus(PaymentStatus.FAILED);
                p.setFailureReason(reason);
                paymentRepository.save(p);
            });

            outboxService.save(saga.getPaymentId(), "PAYMENT",
                    "PAYMENT_REVERSAL_NEEDED",
                    Map.of(
                            "paymentId", saga.getPaymentId().toString(),
                            "reason",    reason
                    )
            );

            log.warn("Stuck DEBITING → REVERSAL_NEEDED. paymentId={}",
                    saga.getPaymentId());
        }
    }
}