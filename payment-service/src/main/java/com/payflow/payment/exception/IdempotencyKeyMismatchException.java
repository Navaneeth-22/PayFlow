package com.payflow.payment.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyKeyMismatchException extends PayflowException {
    public IdempotencyKeyMismatchException() {
        super(
                "Request body does not match original request for this idempotency key",
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}