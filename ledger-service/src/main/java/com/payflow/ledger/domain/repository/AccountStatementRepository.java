package com.payflow.ledger.domain.repository;

import com.payflow.ledger.domain.model.AccountStatement;
import com.payflow.ledger.domain.model.AccountStatementId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountStatementRepository
        extends JpaRepository<AccountStatement, AccountStatementId> {

    Page<AccountStatement> findByIdAccountIdOrderByCreatedAtDesc(
            UUID accountId, Pageable pageable
    );
}