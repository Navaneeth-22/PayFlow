package com.payflow.fraud.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FraudException extends RuntimeException {
    private final HttpStatus status;

    public FraudException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
