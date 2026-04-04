CREATE TABLE saga_states (
                             payment_id      UUID        PRIMARY KEY REFERENCES payments(id),
                             status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                             failure_reason  TEXT,
                             last_event      VARCHAR(100),
                             context         JSONB,
                             created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
                             updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Partial index: stuck-saga detector only scans non-terminal rows
CREATE INDEX idx_saga_stuck ON saga_states(status, updated_at)
    WHERE status NOT IN ('COMPLETED', 'FAILED', 'REVERSED');