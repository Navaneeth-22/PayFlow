ALTER TABLE outbox_events
    ADD COLUMN claimed_at TIMESTAMP,
    ADD COLUMN claimed_by VARCHAR(100);

CREATE INDEX idx_outbox_claimable
    ON outbox_events(created_at ASC)
    WHERE published = FALSE AND claimed_at IS NULL;