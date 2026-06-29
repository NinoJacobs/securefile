ALTER TABLE users
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'));

ALTER TABLE statements
    ADD CONSTRAINT chk_statements_period_range
        CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end);

ALTER TABLE statements
    ADD CONSTRAINT chk_statements_file_size_non_negative
        CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0);

ALTER TABLE account_transactions
    ADD CONSTRAINT chk_account_transactions_reference_not_blank
        CHECK (length(trim(reference)) > 0);

CREATE INDEX IF NOT EXISTS idx_customers_status
    ON customers (status);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_status
    ON accounts (customer_id, status);

CREATE INDEX IF NOT EXISTS idx_account_transactions_account_date_id
    ON account_transactions (account_id, transaction_date DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_statements_customer_period_end
    ON statements (customer_id, period_end DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_statements_customer_account_period_end
    ON statements (customer_id, account_id, period_end DESC);
