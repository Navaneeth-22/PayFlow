package com.payflow.ledger.domain.repository;

import com.payflow.ledger.domain.model.CancelledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CancelledPaymentRepository extends JpaRepository<CancelledPayment, UUID> {

    boolean existsByPaymentId(UUID paymentId);
}