package com.payflow.ledger.exception;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends LedgerException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId, HttpStatus.NOT_FOUND);
    }
}