package com.payflow.payment.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends PayflowException {
    public InsufficientFundsException() {
        super("Insufficient funds in source account", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}