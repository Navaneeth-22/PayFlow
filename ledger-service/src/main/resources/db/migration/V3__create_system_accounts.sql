CREATE TABLE system_accounts (
                                 id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                 code        VARCHAR(50) NOT NULL UNIQUE,
                                 name        TEXT        NOT NULL,
                                 currency    VARCHAR(3)  NOT NULL,
                                 created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

INSERT INTO system_accounts (id, code, name, currency)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'SYSTEM_FLOAT',
           'Platform Float Account',
           'INR'
       );