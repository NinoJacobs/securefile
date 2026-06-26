INSERT INTO users (username, email, password_hash, first_name, last_name, phone, role_id)
VALUES ('customer.three', 'customer.three@securefile.test',
        '$2a$10$YGfslg3v2LQnLWpsr0caOuUeHhiyhg2CV5MBg6NN5RE2zlDzeQ9wK', 'Mia',
        'Khumalo', '+27110000003', (SELECT id FROM roles WHERE name = 'CUSTOMER'));

INSERT INTO customers (user_id, customer_number)
VALUES ((SELECT id FROM users WHERE username = 'customer.three'), 'CUST-0003');

INSERT INTO accounts (customer_id, account_number, account_type, current_balance)
VALUES ((SELECT id FROM customers WHERE customer_number = 'CUST-0003'), '100000000003', 'SAVINGS', 0);

WITH generated_transactions AS (SELECT (SELECT id FROM accounts WHERE account_number = '100000000003') AS account_id,
                                       sequence_number,
                                       DATE '2025-07-03' + ((sequence_number - 1) * 355 / 99)::int     AS transaction_date,
                                       CASE
                                           WHEN sequence_number % 8 = 1 THEN 'Salary Payment'
                                           WHEN sequence_number % 8 = 2 THEN 'Vehicle Finance Debit Order'
                                           WHEN sequence_number % 8 = 3 THEN 'Grocery Payment'
                                           WHEN sequence_number % 8 = 4 THEN 'Cash Withdrawal'
                                           WHEN sequence_number % 8 = 5 THEN 'Fuel Payment'
                                           WHEN sequence_number % 8 = 6 THEN 'Cellphone Debit Order'
                                           WHEN sequence_number % 8 = 7 THEN 'Electricity Purchase'
                                           ELSE 'Online Shopping Payment'
                                           END                                                         AS description,
                                       CASE
                                           WHEN sequence_number % 8 = 1 THEN 'SALARY-' || to_char(
                                                   DATE '2025-07-03' + ((sequence_number - 1) * 355 / 99)::int,
                                                   'YYYYMM')
                                           WHEN sequence_number % 8 = 2 THEN 'VEHICLE-' || to_char(
                                                   DATE '2025-07-03' + ((sequence_number - 1) * 355 / 99)::int,
                                                   'YYYYMM')
                                           WHEN sequence_number % 8 = 3
                                               THEN 'GROCERY-' || lpad(sequence_number::text, 3, '0')
                                           WHEN sequence_number % 8 = 4
                                               THEN 'ATM-' || lpad(sequence_number::text, 3, '0')
                                           WHEN sequence_number % 8 = 5
                                               THEN 'FUEL-' || lpad(sequence_number::text, 3, '0')
                                           WHEN sequence_number % 8 = 6 THEN 'CELL-' || to_char(
                                                   DATE '2025-07-03' + ((sequence_number - 1) * 355 / 99)::int,
                                                   'YYYYMM')
                                           WHEN sequence_number % 8 = 7
                                               THEN 'ELEC-' || lpad(sequence_number::text, 3, '0')
                                           ELSE 'ONLINE-' || lpad(sequence_number::text, 3, '0')
                                           END                                                         AS reference,
                                       CASE
                                           WHEN sequence_number % 8 = 1 THEN 36100.00
                                           WHEN sequence_number % 8 = 2 THEN -6400.00
                                           WHEN sequence_number % 8 = 3
                                               THEN -round((390 + (sequence_number % 12) * 74.80)::numeric, 2)
                                           WHEN sequence_number % 8 = 4
                                               THEN -round((250 + (sequence_number % 6) * 100)::numeric, 2)
                                           WHEN sequence_number % 8 = 5
                                               THEN -round((580 + (sequence_number % 9) * 61.40)::numeric, 2)
                                           WHEN sequence_number % 8 = 6 THEN -899.00
                                           WHEN sequence_number % 8 = 7
                                               THEN -round((350 + (sequence_number % 5) * 80)::numeric, 2)
                                           ELSE -round((300 + (sequence_number % 10) * 64.60)::numeric, 2)
                                           END                                                         AS amount
                                FROM generate_series(1, 100) AS sequence_number),
     balanced_transactions AS (SELECT account_id,
                                      transaction_date,
                                      description,
                                      reference,
                                      amount,
                                      15000.00 + sum(amount) OVER (ORDER BY transaction_date, reference) AS balance_after
                               FROM generated_transactions)
INSERT
INTO account_transactions (account_id, transaction_date, description, reference, amount, balance_after)
SELECT account_id, transaction_date, description, reference, amount, balance_after
FROM balanced_transactions
ORDER BY transaction_date, reference;

UPDATE accounts
SET current_balance = (SELECT balance_after
                       FROM account_transactions
                       WHERE account_id = (SELECT id FROM accounts WHERE account_number = '100000000003')
                       ORDER BY transaction_date DESC, id DESC
                       LIMIT 1)
WHERE account_number = '100000000003';

INSERT INTO statements (customer_id, account_id, statement_name, period_start, period_end, file_key, file_name,
                        file_size_bytes, checksum, generated_at)
VALUES ((SELECT id FROM customers WHERE customer_number = 'CUST-0003'),
        (SELECT id FROM accounts WHERE account_number = '100000000003'),
        '1 Month Statement', '2026-05-01', '2026-05-31', 'statements/CUST-0003/1-month-2026-05.pdf',
        'CUST-0003-1-month-2026-05.pdf', 0, 'seed-cust-0003-1-month-2026-05', '2026-06-01 08:10:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0003'),
        (SELECT id FROM accounts WHERE account_number = '100000000003'),
        '3 Month Statement', '2026-03-01', '2026-05-31', 'statements/CUST-0003/3-month-2026-03-to-2026-05.pdf',
        'CUST-0003-3-month-2026-03-to-2026-05.pdf', 0, 'seed-cust-0003-3-month-2026-03-to-2026-05',
        '2026-06-01 08:10:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0003'),
        (SELECT id FROM accounts WHERE account_number = '100000000003'),
        '6 Month Statement', '2025-12-01', '2026-05-31', 'statements/CUST-0003/6-month-2025-12-to-2026-05.pdf',
        'CUST-0003-6-month-2025-12-to-2026-05.pdf', 0, 'seed-cust-0003-6-month-2025-12-to-2026-05',
        '2026-06-01 08:10:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0003'),
        (SELECT id FROM accounts WHERE account_number = '100000000003'),
        '9 Month Statement', '2025-09-01', '2026-05-31', 'statements/CUST-0003/9-month-2025-09-to-2026-05.pdf',
        'CUST-0003-9-month-2025-09-to-2026-05.pdf', 0, 'seed-cust-0003-9-month-2025-09-to-2026-05',
        '2026-06-01 08:10:00');
