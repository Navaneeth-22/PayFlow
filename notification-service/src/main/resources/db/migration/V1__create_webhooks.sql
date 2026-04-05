CREATE TABLE webhooks (
                          id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                          merchant_id UUID        NOT NULL,
                          url         TEXT        NOT NULL,
                          secret      VARCHAR(255) NOT NULL,
                          events      TEXT[]      NOT NULL,
                          active      BOOLEAN     NOT NULL DEFAULT TRUE,
                          created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhooks_merchant ON webhooks(merchant_id) WHERE active = TRUE;