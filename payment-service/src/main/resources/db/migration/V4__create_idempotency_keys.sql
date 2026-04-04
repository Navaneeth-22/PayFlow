CREATE TABLE idempotency_keys (
                                  key             VARCHAR(255)    PRIMARY KEY,
                                  request_hash    VARCHAR(64)     NOT NULL,
                                  status          VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS',
                                  response_status INTEGER,
                                  response_body   JSONB,
                                  expires_at      TIMESTAMP       NOT NULL,
                                  created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);