package com.payflow.ledger.domain.repository;

import com.payflow.ledger.domain.model.AccountBalance;
import com.payflow.ledger.domain.model.AccountBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository
        extends JpaRepository<AccountBalance, AccountBalanceId> {

    Optional<AccountBalance> findByIdAccountIdAndIdCurrency(
            UUID accountId, String currency
    );
}