CREATE TABLE webhook_deliveries (
                                    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                    webhook_id       UUID        NOT NULL REFERENCES webhooks(id),
                                    event_id         UUID        NOT NULL,
                                    event_type       VARCHAR(100) NOT NULL,
                                    payload          JSONB       NOT NULL,
                                    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                                    attempt_count    INTEGER     NOT NULL DEFAULT 0,
                                    next_attempt_at  TIMESTAMP,
                                    last_http_status INTEGER,
                                    last_response    TEXT,
                                    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
                                    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_delivery_idempotency ON webhook_deliveries(webhook_id, event_id);

CREATE INDEX idx_delivery_retry ON webhook_deliveries(next_attempt_at) WHERE status = 'PENDING';