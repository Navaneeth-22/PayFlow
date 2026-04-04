package com.payflow.payment.domain.repository;

import com.payflow.payment.domain.model.Payment;
import com.payflow.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String key);
    Page<Payment> findByFromAccountId(UUID accountId, Pageable pageable);
    Page<Payment> findByFromAccountIdAndStatus(UUID accountId, PaymentStatus status, Pageable pageable);
}