package com.payflow.ledger.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LedgerException extends RuntimeException {
    private final HttpStatus status;

    public LedgerException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}