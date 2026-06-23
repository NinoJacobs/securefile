CREATE TABLE roles
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    phone         VARCHAR(30),
    role_id       BIGINT       NOT NULL REFERENCES roles (id),
    status        VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role_id
    ON users (role_id);

CREATE TABLE customers
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL UNIQUE REFERENCES users (id),
    customer_number VARCHAR(50) NOT NULL UNIQUE,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts
(
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT        NOT NULL REFERENCES customers (id),
    account_number  VARCHAR(50)   NOT NULL UNIQUE,
    account_type    VARCHAR(50)   NOT NULL,
    current_balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_customer_id
    ON accounts (customer_id);

CREATE TABLE account_transactions
(
    id               BIGSERIAL PRIMARY KEY,
    account_id       BIGINT         NOT NULL REFERENCES accounts (id),
    transaction_date DATE           NOT NULL,
    description      VARCHAR(255)   NOT NULL,
    reference        VARCHAR(100)   NOT NULL,
    amount           NUMERIC(15, 2) NOT NULL,
    balance_after    NUMERIC(15, 2) NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_transactions_account_date
    ON account_transactions (account_id, transaction_date);

CREATE TABLE statements
(
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT       NOT NULL REFERENCES customers (id),
    account_id      BIGINT       NOT NULL REFERENCES accounts (id),
    statement_name  VARCHAR(255) NOT NULL,
    period_start    DATE,
    period_end      DATE,
    file_key        TEXT         NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT,
    content_type    VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    checksum        VARCHAR(128),
    generated_at    TIMESTAMP,
    download_link_expires_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_statements_customer_id
    ON statements (customer_id);

CREATE INDEX idx_statements_customer_period
    ON statements (customer_id, period_start, period_end);
