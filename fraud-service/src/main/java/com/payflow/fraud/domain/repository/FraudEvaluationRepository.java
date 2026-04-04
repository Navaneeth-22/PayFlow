package com.payflow.fraud.domain.repository;

import com.payflow.fraud.domain.model.FraudEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FraudEvaluationRepository extends JpaRepository<FraudEvaluation, UUID> {
    boolean existsByPaymentId(UUID paymentId);
}