-- Dedicated API Sentinel benchmark identities.
-- Existing Alice, Bob, Admin and their accounts remain unchanged.

INSERT INTO bank_users (username, password_hash, role, is_active)
VALUES
(
    'sentinel_alice',
    '$2a$12$5LkMZ2hh.ghNA/ojKib73ezYfzuEVSEnN0SFkV4M5buYka2Bnab5u',
    'CUSTOMER',
    TRUE
),
(
    'sentinel_bob',
    '$2a$12$jdWITCLZwwQuxM0y9F7u8uQ.mzKN.sbUaI8kUIF7fbn1IB7H8AKo.',
    'CUSTOMER',
    TRUE
),
(
    'sentinel_admin',
    '$2a$12$BnRFwUv59jct35p0CZ2aS.iyPclWD/TJHAzhNWjzHIeRdoXzMM1Yu',
    'ADMIN',
    TRUE
);

INSERT INTO accounts
    (account_number, owner_user_id, balance, currency, status)
SELECT
    'SENTINEL-TEST-ALICE-101',
    id,
    25000.00,
    'INR',
    'ACTIVE'
FROM bank_users
WHERE username = 'sentinel_alice';

INSERT INTO accounts
    (account_number, owner_user_id, balance, currency, status)
SELECT
    'SENTINEL-TEST-BOB-202',
    id,
    18000.00,
    'INR',
    'ACTIVE'
FROM bank_users
WHERE username = 'sentinel_bob';