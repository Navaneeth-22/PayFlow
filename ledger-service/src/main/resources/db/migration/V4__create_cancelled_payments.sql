CREATE TABLE cancelled_payments (
                                    payment_id  UUID        PRIMARY KEY,
                                    reason      TEXT,
                                    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);