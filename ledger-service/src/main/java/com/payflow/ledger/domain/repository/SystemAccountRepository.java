package com.payflow.ledger.domain.repository;

import com.payflow.ledger.domain.model.SystemAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SystemAccountRepository extends JpaRepository<SystemAccount, UUID> {
    Optional<SystemAccount> findByCode(String code);
}