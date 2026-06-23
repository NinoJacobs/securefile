INSERT INTO users (username, email, password_hash, first_name, last_name, phone, role_id)
VALUES ('customer.one', 'customer.one@securefile.test', '$2a$10$YGfslg3v2LQnLWpsr0caOuUeHhiyhg2CV5MBg6NN5RE2zlDzeQ9wK', 'Ava', 'Mokoena',
        '+27110000001', (SELECT id FROM roles WHERE name = 'CUSTOMER'));

INSERT INTO customers (user_id, customer_number)
VALUES ((SELECT id FROM users WHERE username = 'customer.one'), 'CUST-0001');

INSERT INTO accounts (customer_id, account_number, account_type, current_balance)
VALUES ((SELECT id FROM customers WHERE customer_number = 'CUST-0001'), '100000000001', 'CHEQUE', 0);

WITH generated_transactions AS (
    SELECT (SELECT id FROM accounts WHERE account_number = '100000000001') AS account_id,
           sequence_number,
           DATE '2025-07-01' + ((sequence_number - 1) * 357 / 99)::int AS transaction_date,
           CASE
               WHEN sequence_number % 8 = 1 THEN 'Salary Payment'
               WHEN sequence_number % 8 = 2 THEN 'Rent Debit Order'
               WHEN sequence_number % 8 = 3 THEN 'Grocery Payment'
               WHEN sequence_number % 8 = 4 THEN 'Cash Withdrawal'
               WHEN sequence_number % 8 = 5 THEN 'Fuel Payment'
               WHEN sequence_number % 8 = 6 THEN 'Insurance Debit Order'
               WHEN sequence_number % 8 = 7 THEN 'Streaming Subscription'
               ELSE 'Pharmacy Card Payment'
           END AS description,
           CASE
               WHEN sequence_number % 8 = 1 THEN 'SALARY-' || to_char(DATE '2025-07-01' + ((sequence_number - 1) * 357 / 99)::int, 'YYYYMM')
               WHEN sequence_number % 8 = 2 THEN 'RENT-' || to_char(DATE '2025-07-01' + ((sequence_number - 1) * 357 / 99)::int, 'YYYYMM')
               WHEN sequence_number % 8 = 3 THEN 'GROCERY-' || lpad(sequence_number::text, 3, '0')
               WHEN sequence_number % 8 = 4 THEN 'ATM-' || lpad(sequence_number::text, 3, '0')
               WHEN sequence_number % 8 = 5 THEN 'FUEL-' || lpad(sequence_number::text, 3, '0')
               WHEN sequence_number % 8 = 6 THEN 'INS-' || to_char(DATE '2025-07-01' + ((sequence_number - 1) * 357 / 99)::int, 'YYYYMM')
               WHEN sequence_number % 8 = 7 THEN 'SUB-' || lpad(sequence_number::text, 3, '0')
               ELSE 'PHARM-' || lpad(sequence_number::text, 3, '0')
           END AS reference,
           CASE
               WHEN sequence_number % 8 = 1 THEN 32500.00
               WHEN sequence_number % 8 = 2 THEN -9200.00
               WHEN sequence_number % 8 = 3 THEN -round((450 + (sequence_number % 11) * 83.45)::numeric, 2)
               WHEN sequence_number % 8 = 4 THEN -round((300 + (sequence_number % 5) * 100)::numeric, 2)
               WHEN sequence_number % 8 = 5 THEN -round((650 + (sequence_number % 7) * 75.30)::numeric, 2)
               WHEN sequence_number % 8 = 6 THEN -1450.00
               WHEN sequence_number % 8 = 7 THEN -199.00
               ELSE -round((120 + (sequence_number % 6) * 42.15)::numeric, 2)
           END AS amount
    FROM generate_series(1, 100) AS sequence_number
),
balanced_transactions AS (
    SELECT account_id,
           transaction_date,
           description,
           reference,
           amount,
           12000.00 + sum(amount) OVER (ORDER BY transaction_date, reference) AS balance_after
    FROM generated_transactions
)
INSERT INTO account_transactions (account_id, transaction_date, description, reference, amount, balance_after)
SELECT account_id, transaction_date, description, reference, amount, balance_after
FROM balanced_transactions
ORDER BY transaction_date, reference;

UPDATE accounts
SET current_balance = (
    SELECT balance_after
    FROM account_transactions
    WHERE account_id = (SELECT id FROM accounts WHERE account_number = '100000000001')
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1
)
WHERE account_number = '100000000001';

INSERT INTO statements (customer_id, account_id, statement_name, period_start, period_end, file_key, file_name,
                        file_size_bytes, checksum, generated_at)
VALUES ((SELECT id FROM customers WHERE customer_number = 'CUST-0001'), (SELECT id FROM accounts WHERE account_number = '100000000001'),
        '1 Month Statement', '2026-05-01', '2026-05-31', 'statements/CUST-0001/1-month-2026-05.pdf',
        'CUST-0001-1-month-2026-05.pdf', 0, 'seed-cust-0001-1-month-2026-05', '2026-06-01 08:00:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0001'), (SELECT id FROM accounts WHERE account_number = '100000000001'),
        '3 Month Statement', '2026-03-01', '2026-05-31', 'statements/CUST-0001/3-month-2026-03-to-2026-05.pdf',
        'CUST-0001-3-month-2026-03-to-2026-05.pdf', 0, 'seed-cust-0001-3-month-2026-03-to-2026-05', '2026-06-01 08:00:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0001'), (SELECT id FROM accounts WHERE account_number = '100000000001'),
        '6 Month Statement', '2025-12-01', '2026-05-31', 'statements/CUST-0001/6-month-2025-12-to-2026-05.pdf',
        'CUST-0001-6-month-2025-12-to-2026-05.pdf', 0, 'seed-cust-0001-6-month-2025-12-to-2026-05', '2026-06-01 08:00:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0001'), (SELECT id FROM accounts WHERE account_number = '100000000001'),
        '9 Month Statement', '2025-09-01', '2026-05-31', 'statements/CUST-0001/9-month-2025-09-to-2026-05.pdf',
        'CUST-0001-9-month-2025-09-to-2026-05.pdf', 0, 'seed-cust-0001-9-month-2025-09-to-2026-05', '2026-06-01 08:00:00');
