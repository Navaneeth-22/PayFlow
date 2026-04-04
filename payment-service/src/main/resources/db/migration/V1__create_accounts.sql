CREATE TABLE accounts (
                          id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id     UUID            NOT NULL UNIQUE,
                          currency    VARCHAR(3)      NOT NULL DEFAULT 'INR',
                          status      VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
                          created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
                          updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);