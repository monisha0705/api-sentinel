-- Synthetic benchmark identities
INSERT INTO bank_users (id, username, password_hash, role, is_active)
VALUES
(
    1,
    'alice',
    '$2a$12$0S38QsdQLqk4hNZQU.76fO3SYxiSUMlvh6AxDJq8RVjQ65mXznd1.',
    'CUSTOMER',
    TRUE
),
(
    2,
    'bob',
    '$2a$12$YO8sFX6.16kaP7yxFrtrH.AY5CnYwrYty1MvRshmb0PR/5wPz6vca',
    'CUSTOMER',
    TRUE
),
(
    3,
    'admin',
    '$2a$12$Nkp7LBVePkraEh64IH9cEeZr5KqwPPmpUz/.XX82A1auJ2nD1tj4a',
    'ADMIN',
    TRUE
);

-- Synthetic customer accounts
INSERT INTO accounts
    (id, account_number, owner_user_id, balance, currency, status)
VALUES
    (101, 'SENTINEL-ALICE-101', 1, 25000.00, 'INR', 'ACTIVE'),
    (202, 'SENTINEL-BOB-202', 2, 18000.00, 'INR', 'ACTIVE');

-- Keep identity sequences aligned with explicitly inserted IDs
SELECT setval(
    pg_get_serial_sequence('bank_users', 'id'),
    (SELECT MAX(id) FROM bank_users)
);

SELECT setval(
    pg_get_serial_sequence('accounts', 'id'),
    (SELECT MAX(id) FROM accounts)
);