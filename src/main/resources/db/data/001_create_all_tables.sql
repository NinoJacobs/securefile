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

CREATE TABLE statements
(
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT       NOT NULL REFERENCES customers (id),
    statement_name  VARCHAR(255) NOT NULL,
    period_start    DATE,
    period_end      DATE,
    file_key        TEXT         NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT,
    content_type    VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    checksum        VARCHAR(128),
    status          VARCHAR(30)  NOT NULL DEFAULT 'AVAILABLE',
    generated_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_statements_customer_id
    ON statements (customer_id);

CREATE INDEX idx_statements_customer_period
    ON statements (customer_id, period_start, period_end);

CREATE TABLE statement_generation_requests
(
    id                   BIGSERIAL PRIMARY KEY,
    customer_id          BIGINT      NOT NULL REFERENCES customers (id),
    requested_by_user_id BIGINT REFERENCES users (id),
    status               VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    period_start         DATE,
    period_end           DATE,
    statement_id         BIGINT REFERENCES statements (id),
    error_message        TEXT,
    requested_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at         TIMESTAMP,
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_statement_generation_requests_customer_id
    ON statement_generation_requests (customer_id);

CREATE INDEX idx_statement_generation_requests_status
    ON statement_generation_requests (status);