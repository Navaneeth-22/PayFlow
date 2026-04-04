CREATE TABLE fraud_evaluations (
                                   id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                                   payment_id      UUID            NOT NULL UNIQUE,  -- UNIQUE: one evaluation per payment
                                   user_id         UUID            NOT NULL,
                                   amount          DECIMAL(19,4)   NOT NULL,
                                   currency        VARCHAR(3)      NOT NULL,
                                   result          VARCHAR(20)     NOT NULL,          -- 'CLEARED' | 'FLAGGED'
                                   flagged_reason  TEXT,                              -- null if CLEARED
                                   rules_checked   TEXT[],                            -- which rules ran
                                   evaluation_ms   INTEGER,                           -- how long the check took
                                   created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_user_id    ON fraud_evaluations(user_id, created_at DESC);
CREATE INDEX idx_fraud_payment_id ON fraud_evaluations(payment_id);
CREATE INDEX idx_fraud_result     ON fraud_evaluations(result) WHERE result = 'FLAGGED';

CREATE TABLE outbox_events (
                               id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                               aggregate_id    UUID            NOT NULL,
                               aggregate_type  VARCHAR(100)    NOT NULL,
                               event_type      VARCHAR(100)    NOT NULL,
                               payload         JSONB           NOT NULL,
                               published       BOOLEAN         NOT NULL DEFAULT FALSE,
                               published_at    TIMESTAMP,
                               created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at ASC)
    WHERE published = FALSE;