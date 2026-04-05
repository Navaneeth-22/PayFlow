package com.payflow.ledger.domain.repository;

import com.payflow.ledger.domain.model.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    boolean existsByPaymentId(UUID paymentId);

    boolean existsByEntryRef(String entryRef);
}