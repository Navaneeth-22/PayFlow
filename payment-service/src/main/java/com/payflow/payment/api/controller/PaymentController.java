package com.payflow.payment.api.controller;

import com.payflow.payment.api.dto.CreatePaymentRequest;
import com.payflow.payment.api.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        log.info("Payment request received. idempotencyKey={}, amount={}",
                idempotencyKey, request.getAmount());

        PaymentResponse stub = PaymentResponse.builder()
                .paymentId(UUID.randomUUID())
                .status("PENDING")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(stub);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.notFound().build();
    }
}