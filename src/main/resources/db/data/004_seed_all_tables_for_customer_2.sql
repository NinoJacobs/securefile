INSERT INTO users (username, email, password_hash, first_name, last_name, phone, role_id)
VALUES ('customer.two', 'customer.two@securefile.test', '$2a$10$YGfslg3v2LQnLWpsr0caOuUeHhiyhg2CV5MBg6NN5RE2zlDzeQ9wK',
        'Liam', 'Naidoo',
        '+27110000002', (SELECT id FROM roles WHERE name = 'CUSTOMER'));

INSERT INTO customers (user_id, customer_number)
VALUES ((SELECT id FROM users WHERE username = 'customer.two'), 'CUST-0002');

INSERT INTO accounts (customer_id, account_number, account_type, current_balance)
VALUES ((SELECT id FROM customers WHERE customer_number = 'CUST-0002'), '100000000002', 'CHEQUE', 0);

WITH generated_transactions AS (SELECT (SELECT id FROM accounts WHERE account_number = '100000000002') AS account_id,
                                       sequence_number,
                                       DATE '2025-07-02' + ((sequence_number - 1) * 356 / 99)::int     AS transaction_date,
                                       CASE
                                           WHEN sequence_number % 8 = 1 THEN 'Salary Payment'
                                           WHEN sequence_number % 8 = 2 THEN 'Home Loan Debit Order'
                                           WHEN sequence_number % 8 = 3 THEN 'Grocery Payment'
                                           WHEN sequence_number % 8 = 4 THEN 'Cash Withdrawal'
                                           WHEN sequence_number % 8 = 5 THEN 'Fuel Payment'
                                           WHEN sequence_number % 8 = 6 THEN 'Medical Aid Debit Order'
                                           WHEN sequence_number % 8 = 7 THEN 'School Fees Payment'
                                           ELSE 'Restaurant Card Payment'
                                           END                                                         AS description,
                                       CASE
                                           WHEN sequence_number % 8 = 1 THEN 'SALARY-' || to_char(
                                                   DATE '2025-07-02' + ((sequence_number - 1) * 356 / 99)::int,
                                                   'YYYYMM')
                                           WHEN sequence_number % 8 = 2 THEN 'BOND-' || to_char(
                                                   DATE '2025-07-02' + ((sequence_number - 1) * 356 / 99)::int,
                                                   'YYYYMM')
                                           WHEN sequence_number % 8 = 3
                                               THEN 'GROCERY-' || lpad(sequence_number::text, 3, '0')
                                           WHEN sequence_number % 8 = 4
                                               THEN 'ATM-' || lpad(sequence_number::text, 3, '0')
                                           WHEN sequence_number % 8 = 5
                                               THEN 'FUEL-' || lpad(sequence_number::text, 3, '0')
                                           WHEN sequence_number % 8 = 6 THEN 'MED-' || to_char(
                                                   DATE '2025-07-02' + ((sequence_number - 1) * 356 / 99)::int,
                                                   'YYYYMM')
                                           WHEN sequence_number % 8 = 7
                                               THEN 'SCHOOL-' || lpad(sequence_number::text, 3, '0')
                                           ELSE 'REST-' || lpad(sequence_number::text, 3, '0')
                                           END                                                         AS reference,
                                       CASE
                                           WHEN sequence_number % 8 = 1 THEN 28750.00
                                           WHEN sequence_number % 8 = 2 THEN -11200.00
                                           WHEN sequence_number % 8 = 3
                                               THEN -round((520 + (sequence_number % 13) * 69.35)::numeric, 2)
                                           WHEN sequence_number % 8 = 4
                                               THEN -round((400 + (sequence_number % 4) * 150)::numeric, 2)
                                           WHEN sequence_number % 8 = 5
                                               THEN -round((720 + (sequence_number % 8) * 68.90)::numeric, 2)
                                           WHEN sequence_number % 8 = 6 THEN -3180.00
                                           WHEN sequence_number % 8 = 7 THEN -2100.00
                                           ELSE -round((260 + (sequence_number % 7) * 55.25)::numeric, 2)
                                           END                                                         AS amount
                                FROM generate_series(1, 100) AS sequence_number),
     balanced_transactions AS (SELECT account_id,
                                      transaction_date,
                                      description,
                                      reference,
                                      amount,
                                      8500.00 + sum(amount) OVER (ORDER BY transaction_date, reference) AS balance_after
                               FROM generated_transactions)
INSERT
INTO account_transactions (account_id, transaction_date, description, reference, amount, balance_after)
SELECT account_id, transaction_date, description, reference, amount, balance_after
FROM balanced_transactions
ORDER BY transaction_date, reference;

UPDATE accounts
SET current_balance = (SELECT balance_after
                       FROM account_transactions
                       WHERE account_id = (SELECT id FROM accounts WHERE account_number = '100000000002')
                       ORDER BY transaction_date DESC, id DESC
                       LIMIT 1)
WHERE account_number = '100000000002';

INSERT INTO statements (customer_id, account_id, statement_name, period_start, period_end, file_key, file_name,
                        file_size_bytes, checksum, generated_at)
VALUES ((SELECT id FROM customers WHERE customer_number = 'CUST-0002'),
        (SELECT id FROM accounts WHERE account_number = '100000000002'),
        '1 Month Statement', '2026-05-01', '2026-05-31', 'statements/CUST-0002/1-month-2026-05.pdf',
        'CUST-0002-1-month-2026-05.pdf', 0, 'seed-cust-0002-1-month-2026-05', '2026-06-01 08:05:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0002'),
        (SELECT id FROM accounts WHERE account_number = '100000000002'),
        '3 Month Statement', '2026-03-01', '2026-05-31', 'statements/CUST-0002/3-month-2026-03-to-2026-05.pdf',
        'CUST-0002-3-month-2026-03-to-2026-05.pdf', 0, 'seed-cust-0002-3-month-2026-03-to-2026-05',
        '2026-06-01 08:05:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0002'),
        (SELECT id FROM accounts WHERE account_number = '100000000002'),
        '6 Month Statement', '2025-12-01', '2026-05-31', 'statements/CUST-0002/6-month-2025-12-to-2026-05.pdf',
        'CUST-0002-6-month-2025-12-to-2026-05.pdf', 0, 'seed-cust-0002-6-month-2025-12-to-2026-05',
        '2026-06-01 08:05:00'),
       ((SELECT id FROM customers WHERE customer_number = 'CUST-0002'),
        (SELECT id FROM accounts WHERE account_number = '100000000002'),
        '9 Month Statement', '2025-09-01', '2026-05-31', 'statements/CUST-0002/9-month-2025-09-to-2026-05.pdf',
        'CUST-0002-9-month-2025-09-to-2026-05.pdf', 0, 'seed-cust-0002-9-month-2025-09-to-2026-05',
        '2026-06-01 08:05:00');
