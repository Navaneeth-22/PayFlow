package com.payflow.payment.saga;

import com.payflow.payment.domain.model.*;
import com.payflow.payment.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StuckSagaDetector {

    private final SagaStateRepository sagaStateRepository;
    private final PaymentRepository paymentRepository;

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
        log.warn("Stuck saga detected: paymentId={} status={} stuckSince={}",
                saga.getPaymentId(), saga.getStatus(), saga.getUpdatedAt());

        String reason = String.format(
                "Saga timed out in %s state after %d minutes. " + "Expected event never arrived.",
                saga.getStatus().name(),
                STUCK_THRESHOLD.toMinutes()
        );

        saga.setStatus(SagaStatus.FAILED);
        saga.setLastEvent("STUCK_SAGA_TIMEOUT");
        saga.setFailureReason(reason);
        sagaStateRepository.save(saga);

        paymentRepository.findById(saga.getPaymentId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
        });

        log.warn("Saga force-failed: paymentId={} reason={}",
                saga.getPaymentId(), reason);
    }
}