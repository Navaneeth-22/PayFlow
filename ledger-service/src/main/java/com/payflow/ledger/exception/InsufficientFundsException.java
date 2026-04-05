package com.payflow.ledger.exception;
import org.springframework.http.HttpStatus;


public class InsufficientFundsException extends LedgerException {
    public InsufficientFundsException(String balance, String required) {
        super("Insufficient funds. Balance: " + balance + ", Required: " + required,
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}