package com.payflow.payment.domain.model;

public enum SagaStatus {
    PENDING,
    FRAUD_CHECK,
    DEBITING,
    COMPLETED,
    FAILED,
    REVERSAL_NEEDED,
    REVERSED
}