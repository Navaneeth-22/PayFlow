package com.payflow.payment.domain.repository;

import com.payflow.payment.domain.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    List<SagaState> findByStatusInAndUpdatedAtBefore(
            List<SagaStatus> statuses, Instant threshold);
}