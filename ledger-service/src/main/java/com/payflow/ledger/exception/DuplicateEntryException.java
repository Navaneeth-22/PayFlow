package com.payflow.ledger.exception;
import org.springframework.http.HttpStatus;

public class DuplicateEntryException extends LedgerException {
    public DuplicateEntryException(String reference) {
        super("Duplicate entry reference: " + reference, HttpStatus.CONFLICT);
    }
}