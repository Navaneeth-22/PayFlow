package com.payflow.fraud.domain.service;

import lombok.Getter;
@Getter
public class FraudResult {

    private final boolean flagged;
    private final String reason;

    private FraudResult(boolean flagged, String reason) {
        this.flagged = flagged;
        this.reason = reason;
    }

    public static FraudResult cleared() {
        return new FraudResult(false, null);
    }

    public static FraudResult flagged(String reason) {
        return new FraudResult(true, reason);
    }
}