package com.payflow.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends PayflowException {
    public PaymentNotFoundException(String id) {
        super("Payment not found: " + id, HttpStatus.NOT_FOUND);
    }
}