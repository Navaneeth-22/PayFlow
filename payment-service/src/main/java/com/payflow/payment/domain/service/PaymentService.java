package com.payflow.payment.domain.service;

import com.payflow.payment.api.dto.CreatePaymentRequest;
import com.payflow.payment.api.dto.PaymentResponse;
import com.payflow.payment.domain.model.*;
import com.payflow.payment.domain.repository.*;
import com.payflow.payment.exception.AccountNotFoundException;
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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OutboxService outboxService;

    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request,
                                           String idempotencyKey) {

        Account sourceAccount = accountRepository
                .findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        request.getFromAccountId().toString()));

        Account destAccount = accountRepository
                .findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        request.getToAccountId().toString()));

        if (sourceAccount.getId().equals(destAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .fromAccountId(sourceAccount.getId())
                .toAccountId(destAccount.getId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PROCESSING)
                .metadata(request.getMetadata())
                .build();
        paymentRepository.save(payment);

        SagaState sagaState = SagaState.builder()
                .paymentId(payment.getId())
                .status(SagaStatus.FRAUD_CHECK)
                .lastEvent("PAYMENT_INITIATED")
                .build();
        sagaStateRepository.save(sagaState);

        outboxService.save(
                payment.getId(),
                "PAYMENT",
                "PAYMENT_INITIATED",
                buildPaymentPayload(payment, sourceAccount.getUserId())
        );

        log.info("Payment initiated: id={} amount={} idempotencyKey={}",
                payment.getId(), payment.getAmount(), idempotencyKey);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.payflow.payment.exception
                        .PaymentNotFoundException(paymentId.toString()));
        return toResponse(payment);
    }

    private Map<String, Object> buildPaymentPayload(Payment payment, UUID userId) {
        return Map.of(
                "paymentId",     payment.getId().toString(),
                "fromAccountId", payment.getFromAccountId().toString(),
                "toAccountId",   payment.getToAccountId().toString(),
                "userId",        userId.toString(),
                "amount",        payment.getAmount(),
                "currency",      payment.getCurrency()
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .fromAccountId(payment.getFromAccountId())
                .toAccountId(payment.getToAccountId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}