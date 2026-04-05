CREATE TABLE journal_entries (
                                 id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                                 payment_id      UUID            NOT NULL,
                                 account_id      UUID            NOT NULL,
                                 counterpart_id  UUID            NOT NULL,
                                 entry_type      VARCHAR(10)     NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
                                 amount          DECIMAL(19,4)   NOT NULL CHECK (amount > 0),
                                 currency        VARCHAR(3)      NOT NULL,
                                 entry_ref       VARCHAR(255)    NOT NULL UNIQUE,
                                 narration       TEXT,
                                 created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_journal_account  ON journal_entries(account_id, created_at DESC);
CREATE INDEX idx_journal_payment  ON journal_entries(payment_id);

CREATE VIEW account_balances AS
SELECT
    account_id,
    currency,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount
             ELSE -amount END)      AS balance,
    COUNT(*)                        AS total_entries,
    MAX(created_at)                 AS last_entry_at
FROM journal_entries
GROUP BY account_id, currency;

CREATE VIEW account_statements AS
SELECT
    account_id,
    payment_id,
    entry_type,
    amount,
    currency,
    narration,
    created_at,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount
             ELSE -amount END)
        OVER (PARTITION BY account_id
                  ORDER BY created_at
                  ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) AS running_balance
FROM journal_entries
ORDER BY account_id, created_at;