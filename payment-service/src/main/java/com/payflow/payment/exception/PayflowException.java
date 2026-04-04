package com.payflow.payment.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;
@Getter
public class PayflowException extends RuntimeException {

    private final HttpStatus status;

    public PayflowException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}