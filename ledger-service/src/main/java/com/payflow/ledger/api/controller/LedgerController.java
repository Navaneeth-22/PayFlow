package com.payflow.ledger.api.controller;

import com.payflow.ledger.api.dto.BalanceResponse;
import com.payflow.ledger.api.dto.StatementResponse;
import com.payflow.ledger.domain.service.LedgerService;
import com.payflow.ledger.exception.LedgerException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Slf4j
public class LedgerController {

    private final LedgerService ledgerService;


    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "INR") String currency) {

        log.debug("Balance request for accountId: {} currency: {}", accountId, currency);
        BalanceResponse response = ledgerService.getBalanceResponse(accountId, currency);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/statement")
    public ResponseEntity<StatementResponse> getStatement(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "INR") String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0) {
            throw new LedgerException("Page number cannot be negative", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > 100) {
            throw new LedgerException("Page size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }

        log.debug("Statement request for accountId: {} currency: {} page: {} size: {}",
                accountId, currency, page, size);

        StatementResponse response = ledgerService.getStatementResponse(accountId, currency, page, size);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/{accountId}/credit")
    public ResponseEntity<BalanceResponse> credit(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditRequest request) {

        log.info("Credit request for accountId: {} amount: {} reference: {}",
                accountId, request.getAmount(), request.getReference());

        ledgerService.creditAccount(
                accountId,
                request.getAmount(),
                request.getCurrency(),
                request.getReference()
        );

        BalanceResponse balance =
                ledgerService.getBalanceResponse(accountId, request.getCurrency());

        return ResponseEntity.status(HttpStatus.CREATED).body(balance);
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyResponse> verify() {
        BigDecimal invariant = ledgerService.verifyDoubleEntryInvariant();
        boolean valid = BigDecimal.ZERO.compareTo(invariant) == 0;

        log.info("Double-entry verification: valid={} sum={}", valid, invariant);

        return ResponseEntity.ok(VerifyResponse.builder()
                .valid(valid)
                .sum(invariant)
                .message(valid
                        ? "Double-entry invariant holds. All books balance."
                        : "INVARIANT VIOLATED — books do not balance! Sum = " + invariant)
                .build());
    }

    @Data
    public static class CreditRequest {

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        private BigDecimal amount;

        @NotBlank(message = "currency is required")
        private String currency;

        @NotBlank(message = "reference is required")
        private String reference;
    }

    @lombok.Value
    @lombok.Builder
    public static class VerifyResponse {
        boolean valid;
        BigDecimal sum;
        String message;
    }
}