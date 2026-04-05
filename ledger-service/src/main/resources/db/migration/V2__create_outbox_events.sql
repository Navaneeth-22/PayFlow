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