CREATE TABLE payments (
                          id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                          idempotency_key   VARCHAR(255)    NOT NULL UNIQUE,
                          from_account_id   UUID            NOT NULL REFERENCES accounts(id),
                          to_account_id     UUID            NOT NULL REFERENCES accounts(id),
                          amount            DECIMAL(19,4)   NOT NULL CHECK (amount > 0),
                          currency          VARCHAR(3)      NOT NULL DEFAULT 'INR',
                          status            VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
                          failure_reason    TEXT,
                          gateway_ref       VARCHAR(255),
                          parent_payment_id UUID            REFERENCES payments(id),
                          metadata          JSONB,
                          created_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
                          updated_at        TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_from_account ON payments(from_account_id, created_at DESC);
CREATE INDEX idx_payments_status       ON payments(status)
    WHERE status IN ('PENDING', 'PROCESSING');