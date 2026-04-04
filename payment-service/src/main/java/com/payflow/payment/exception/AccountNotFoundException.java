package com.payflow.payment.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends PayflowException {
    public AccountNotFoundException(String id) {
        super("Account not found: " + id, HttpStatus.NOT_FOUND);
    }
}