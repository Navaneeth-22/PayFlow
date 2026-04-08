package com.payflow.ledger.domain.service;

import com.payflow.ledger.api.dto.BalanceResponse;
import com.payflow.ledger.api.dto.StatementLineResponse;
import com.payflow.ledger.api.dto.StatementResponse;
import com.payflow.ledger.domain.model.*;
import com.payflow.ledger.domain.repository.*;
import com.payflow.ledger.exception.DuplicateEntryException;
import com.payflow.ledger.exception.LedgerException;
import com.payflow.ledger.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountStatementRepository accountStatementRepository;
    private final SystemAccountRepository systemAccountRepository;
    private final OutboxService outboxService;
    private final CancelledPaymentRepository cancelledPaymentRepository;

    @Transactional
    public void processPayment(Map<String, Object> event) {
        UUID paymentId     = UUID.fromString((String) event.get("paymentId"));
        UUID fromAccountId = UUID.fromString((String) event.get("fromAccountId"));
        UUID toAccountId   = UUID.fromString((String) event.get("toAccountId"));
        BigDecimal amount  = new BigDecimal(event.get("amount").toString());
        String currency    = (String) event.get("currency");

        if (cancelledPaymentRepository.existsByPaymentId(paymentId)) {
            log.warn("Rejecting fraud.cleared for CANCELLED payment: paymentId={}",
                    paymentId);
            outboxService.save(paymentId, "LEDGER", "LEDGER_FAILED", Map.of(
                    "paymentId", paymentId.toString(),
                    "reason",    "Payment was cancelled before ledger processing"
            ));
            return;
        }
        if (journalEntryRepository.existsByPaymentId(paymentId)) {
            log.info("Skipping duplicate for paymentId: {}", paymentId);
            return;
        }


        BigDecimal currentBalance = getBalance(fromAccountId, currency);

        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Insufficient funds for paymentId: {} balance: {} amount: {}",
                    paymentId, currentBalance, amount);
            outboxService.save(paymentId, "LEDGER", "LEDGER_FAILED", Map.of(
                    "paymentId", paymentId.toString(),
                    "reason", "Insufficient funds. Balance: " + currentBalance
                            + ", Required: " + amount
            ));
            return;
        }

        JournalEntry debit = JournalEntry.builder()
                .paymentId(paymentId)
                .accountId(fromAccountId)
                .counterpartId(toAccountId)
                .entryType("DEBIT")
                .amount(amount)
                .currency(currency)
                .entryRef("PAY-" + paymentId + "-DEBIT")
                .narration("Payment to account " + toAccountId)
                .build();

        JournalEntry credit = JournalEntry.builder()
                .paymentId(paymentId)
                .accountId(toAccountId)
                .counterpartId(fromAccountId)
                .entryType("CREDIT")
                .amount(amount)
                .currency(currency)
                .entryRef("PAY-" + paymentId + "-CREDIT")
                .narration("Payment from account " + fromAccountId)
                .build();

        journalEntryRepository.saveAll(List.of(debit, credit));

        outboxService.save(paymentId, "LEDGER", "LEDGER_DEBITED", Map.of(
                "paymentId",     paymentId.toString(),
                "fromAccountId", fromAccountId.toString(),
                "toAccountId",   toAccountId.toString(),
                "amount",        amount,
                "currency",      currency
        ));

        log.info("Journal entries created for paymentId: {}", paymentId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID accountId, String currency) {
        return accountBalanceRepository
                .findByIdAccountIdAndIdCurrency(accountId, currency)
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<AccountStatement> getStatement(UUID accountId, Pageable pageable) {
        return accountStatementRepository
                .findByIdAccountIdOrderByCreatedAtDesc(accountId, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal verifyDoubleEntryInvariant() {
        return journalEntryRepository.findAll().stream()
                .map(e -> e.getEntryType().equals("CREDIT")
                        ? e.getAmount()
                        : e.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void creditAccount(UUID accountId, BigDecimal amount,
                              String currency, String reference) {
        SystemAccount float_ = systemAccountRepository
                .findByCode("SYSTEM_FLOAT")
                .orElseThrow(() -> new LedgerException("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));

        String creditRef = "TOPUP-" + reference + "-USER-CREDIT";
        if (journalEntryRepository.existsByEntryRef(creditRef)) {
            log.info("Skipping duplicate top-up for reference: {}", reference);
            throw new DuplicateEntryException(reference);
        }

        journalEntryRepository.saveAll(List.of(
                JournalEntry.builder()
                        .paymentId(UUID.nameUUIDFromBytes(reference.getBytes()))
                        .accountId(float_.getId())
                        .counterpartId(accountId)
                        .entryType("DEBIT")
                        .amount(amount).currency(currency)
                        .entryRef("TOPUP-" + reference + "-FLOAT-DEBIT")
                        .narration("Top-up to user account " + accountId)
                        .build(),
                JournalEntry.builder()
                        .paymentId(UUID.nameUUIDFromBytes(reference.getBytes()))
                        .accountId(accountId)
                        .counterpartId(float_.getId())
                        .entryType("CREDIT")
                        .amount(amount).currency(currency)
                        .entryRef(creditRef)
                        .narration("Top-up received")
                        .build()
        ));

        log.info("Account {} credited with {} {}", accountId, amount, currency);
    }

    @Transactional
    public BalanceResponse getBalanceResponse(UUID accountId, String currency){
        return accountBalanceRepository
                .findByIdAccountIdAndIdCurrency(accountId,currency)
                .map(b -> BalanceResponse.builder()
                        .accountId(accountId)
                        .currency(currency)
                        .balance(b.getBalance())
                        .totalEntries(b.getTotalEntries())
                        .lastEntryAt(b.getLastEntryAt())
                        .build())
                .orElse(BalanceResponse.builder()
                        .accountId(accountId)
                        .currency(currency)
                        .balance(BigDecimal.ZERO)
                        .totalEntries(0L)
                        .build());
    }


    @Transactional
    public StatementResponse getStatementResponse(UUID accountId,
                                                  String currency,
                                                  int page,
                                                  int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<AccountStatement> statementPage = accountStatementRepository
                .findByIdAccountIdOrderByCreatedAtDesc(accountId, pageable);

        BigDecimal currentBalance = getBalance(accountId, currency);

        List<StatementLineResponse> lines = statementPage.getContent().stream()
                .map(s -> StatementLineResponse.builder()
                        .paymentId(s.getId().getPaymentId())
                        .entryType(s.getId().getEntryType())
                        .amount(s.getAmount())
                        .currency(s.getCurrency())
                        .narration(s.getNarration())
                        .runningBalance(s.getRunningBalance())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        return StatementResponse.builder()
                .accountId(accountId)
                .currency(currency)
                .currentBalance(currentBalance)
                .entries(lines)
                .page(page)
                .totalPages(statementPage.getTotalPages())
                .totalEntries(statementPage.getTotalElements())
                .build();
    }

    @Transactional
    public void reversePayment(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        if (!journalEntryRepository.existsByPaymentId(paymentId)) {
            log.info("No journal entries found for reversal. " +
                    "Publishing ledger.reversed as no-op. paymentId={}", paymentId);
            outboxService.save(paymentId, "LEDGER", "LEDGER_REVERSED", Map.of(
                    "paymentId", paymentId.toString(),
                    "reason",    "No entries to reverse"
            ));
            return;
        }

        String reversalRef = "REVERSAL-" + paymentId + "-CREDIT";
        if (journalEntryRepository.existsByEntryRef(reversalRef)) {
            log.info("Reversal already processed for paymentId={}. Skipping.",
                    paymentId);
            return;
        }

        List<JournalEntry> originalEntries = journalEntryRepository
                .findByPaymentId(paymentId);

        JournalEntry originalDebit = originalEntries.stream()
                .filter(e -> "DEBIT".equals(e.getEntryType()))
                .findFirst()
                .orElse(null);

        if (originalDebit == null) {
            log.error("Cannot reverse — no DEBIT entry found for paymentId={}",
                    paymentId);
            return;
        }

        UUID sourceAccountId = originalDebit.getAccountId();
        UUID destAccountId   = originalDebit.getCounterpartId();
        BigDecimal amount    = originalDebit.getAmount();
        String currency      = originalDebit.getCurrency();


        JournalEntry reversalCredit = JournalEntry.builder()
                .paymentId(paymentId)
                .accountId(sourceAccountId)
                .counterpartId(destAccountId)
                .entryType("CREDIT")
                .amount(amount)
                .currency(currency)
                .entryRef("REVERSAL-" + paymentId + "-CREDIT")
                .narration("Reversal: credit back to source account")
                .build();


        JournalEntry reversalDebit = JournalEntry.builder()
                .paymentId(paymentId)
                .accountId(destAccountId)
                .counterpartId(sourceAccountId)
                .entryType("DEBIT")
                .amount(amount)
                .currency(currency)
                .entryRef("REVERSAL-" + paymentId + "-DEBIT")
                .narration("Reversal: debit back from destination account")
                .build();

        journalEntryRepository.saveAll(List.of(reversalCredit, reversalDebit));

        outboxService.save(paymentId, "LEDGER", "LEDGER_REVERSED", Map.of(
                "paymentId", paymentId.toString(),
                "amount",    amount,
                "currency",  currency
        ));

        log.info("Reversal complete for paymentId={} amount={} {}",
                paymentId, amount, currency);
    }

    @Transactional
    public void cancelPayment(Map<String,Object> event){
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        String reason = (String) event.get("reason");
        if (!cancelledPaymentRepository.existsByPaymentId(paymentId)) {
            cancelledPaymentRepository.save(
                    CancelledPayment.builder()
                            .paymentId(paymentId)
                            .reason(reason)
                            .build()
            );
            log.info("Payment recorded as cancelled in ledger: paymentId={}",
                    paymentId);
        }
    }


}