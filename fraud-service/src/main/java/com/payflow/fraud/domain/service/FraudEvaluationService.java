package com.payflow.fraud.domain.service;

import com.payflow.fraud.domain.model.FraudEvaluation;
import com.payflow.fraud.domain.repository.FraudEvaluationRepository;
import com.payflow.fraud.exception.FraudException;
import com.payflow.fraud.messaging.dto.PaymentInitiatedEvent;
import com.payflow.fraud.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEvaluationService {

    private final FraudEvaluationRepository fraudEvaluationRepository;
    private final OutboxService outboxService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${fraud.rules.max-tx-per-60s:5}")
    private int maxTxPer60s;

    @Value("${fraud.rules.single-tx-limit:100000}")
    private BigDecimal singleTxLimit;

    @Value("${fraud.rules.max-amount-per-hour:500000}")
    private BigDecimal maxAmountPerHour;
    @Transactional
    public void evaluate(PaymentInitiatedEvent event) {
        long startMs = System.currentTimeMillis();
        if (fraudEvaluationRepository.existsByPaymentId(event.getPaymentId())) {
            log.info("Skipping duplicate fraud evaluation for paymentId: {}",
                    event.getPaymentId());
            return;
        }

        if (event.getPaymentId() == null || event.getAmount() == null) {
            throw new FraudException(
                    "Malformed PAYMENT_INITIATED event — missing required fields",
                    HttpStatus.BAD_REQUEST
            );
        }
        List<String> rulesChecked = new ArrayList<>();
        FraudResult result = runRules(event, rulesChecked);

        long evaluationMs = System.currentTimeMillis() - startMs;

        FraudEvaluation evaluation = FraudEvaluation.builder()
                .paymentId(event.getPaymentId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .result(result.isFlagged() ? "FLAGGED" : "CLEARED")
                .flaggedReason(result.getReason())
                .rulesChecked(rulesChecked)
                .evaluationMs((int) evaluationMs)
                .build();

        fraudEvaluationRepository.save(evaluation);
        String eventType = result.isFlagged() ? "FRAUD_FLAGGED" : "FRAUD_CLEARED";
        outboxService.save(
                event.getPaymentId(),
                "FRAUD",
                eventType,
                buildPayload(event, result)
        );

        log.info("Fraud evaluation: paymentId={} result={} reason={} ms={}",
                event.getPaymentId(), eventType, result.getReason(), evaluationMs);
    }

    private FraudResult runRules(PaymentInitiatedEvent event, List<String> rulesChecked) {
        rulesChecked.add("SINGLE_TX_LIMIT");
        if (event.getAmount().compareTo(singleTxLimit) > 0) {
            return FraudResult.flagged(
                    "Single tx limit exceeded: " + event.getAmount() + " > " + singleTxLimit
            );
        }

        rulesChecked.add("VELOCITY_COUNT_60S");
        String countKey = "fraud:count:" + event.getUserId();
        long now = Instant.now().toEpochMilli();
        long window60s = now - 60_000L;

        redisTemplate.opsForZSet().add(countKey, String.valueOf(now), now);
        redisTemplate.opsForZSet().removeRangeByScore(countKey, 0, window60s);
        redisTemplate.expire(countKey, Duration.ofMinutes(5));

        Long countInWindow = redisTemplate.opsForZSet().count(countKey, window60s, now);
        if (countInWindow != null && countInWindow > maxTxPer60s) {
            return FraudResult.flagged(
                    "Velocity exceeded: " + countInWindow + " tx in 60s (max " + maxTxPer60s + ")"
            );
        }

        rulesChecked.add("VELOCITY_AMOUNT_1H");
        String amountKey = "fraud:amount:" + event.getUserId();
        long window1h = now - 3_600_000L;

        String amountEntry = event.getAmount().toPlainString() + ":" + now;
        redisTemplate.opsForZSet().add(amountKey, amountEntry, now);
        redisTemplate.opsForZSet().removeRangeByScore(amountKey, 0, window1h);
        redisTemplate.expire(amountKey, Duration.ofHours(2));

        var amountsInWindow = redisTemplate.opsForZSet().rangeByScore(amountKey, window1h, now);
        if (amountsInWindow != null) {
            BigDecimal totalInHour = amountsInWindow.stream()
                    .map(s -> new BigDecimal(s.toString().split(":")[0]))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalInHour.compareTo(maxAmountPerHour) > 0) {
                return FraudResult.flagged(
                        "Amount velocity exceeded: ₹" + totalInHour + " in 1h (max ₹" + maxAmountPerHour + ")"
                );
            }
        }

        return FraudResult.cleared();
    }

    private Map<String, Object> buildPayload(PaymentInitiatedEvent event, FraudResult result) {
        return Map.of(
                "paymentId",      event.getPaymentId().toString(),
                "fromAccountId",  event.getFromAccountId().toString(),
                "toAccountId",    event.getToAccountId().toString(),
                "amount",         event.getAmount(),
                "currency",       event.getCurrency(),
                "result",         result.isFlagged() ? "FLAGGED" : "CLEARED",
                "reason",         result.getReason() != null ? result.getReason() : ""
        );
    }
}