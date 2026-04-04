package com.payflow.payment.exception;

import org.springframework.http.HttpStatus;

public class MissingIdempotencyKeyException extends PayflowException {
    public MissingIdempotencyKeyException() {
        super("X-Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
    }
}