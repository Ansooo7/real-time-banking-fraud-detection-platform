-- =========================================================================
-- Initial Seed Data: System Users, UK Customers, Accounts, Merchants
-- =========================================================================

-- 1. Default System Users (Password for all demo accounts is: Password123!)
INSERT INTO users (id, username, password_hash, email, full_name, role, enabled)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'admin', '$2a$10$1lH3ueg5JzDWNwkUL1m4bOWJpYYUUj30iKQHHUXZX0ELg2yYGux92', 'admin@ukbank.co.uk', 'System Administrator', 'ROLE_ADMIN', true),
    ('22222222-2222-2222-2222-222222222222', 'analyst', '$2a$10$1lH3ueg5JzDWNwkUL1m4bOWJpYYUUj30iKQHHUXZX0ELg2yYGux92', 'sarah.analyst@ukbank.co.uk', 'Sarah Jenkins (Lead Fraud Analyst)', 'ROLE_FRAUD_ANALYST', true),
    ('33333333-3333-3333-3333-333333333333', 'customer', '$2a$10$1lH3ueg5JzDWNwkUL1m4bOWJpYYUUj30iKQHHUXZX0ELg2yYGux92', 'oliver.twist@gmail.com', 'Oliver Twist', 'ROLE_CUSTOMER', true);

-- 2. UK Customers
INSERT INTO customers (id, customer_number, first_name, last_name, email, phone_number, home_city, home_country, risk_tier)
VALUES
    ('c0000001-0000-0000-0000-000000000001', 'CUST-UK-1001', 'Oliver', 'Twist', 'oliver.twist@gmail.com', '+447911123456', 'London', 'GB', 'LOW'),
    ('c0000002-0000-0000-0000-000000000002', 'CUST-UK-1002', 'Emma', 'Watson', 'emma.watson@oxford.ac.uk', '+447922234567', 'Oxford', 'GB', 'LOW'),
    ('c0000003-0000-0000-0000-000000000003', 'CUST-UK-1003', 'James', 'Bond', 'james.bond@mi6.gov.uk', '+447933345678', 'London', 'GB', 'MEDIUM'),
    ('c0000004-0000-0000-0000-000000000004', 'CUST-UK-1004', 'Arthur', 'Shelby', 'arthur.shelby@peaky.co.uk', '+447944456789', 'Birmingham', 'GB', 'HIGH');

-- 3. Customer Bank Accounts (Sort Code + 8-digit Account Number)
INSERT INTO accounts (id, customer_id, account_number, sort_code, currency, balance, status)
VALUES
    ('a0000001-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001', '12345678', '204514', 'GBP', 15420.50, 'ACTIVE'),
    ('a0000002-0000-0000-0000-000000000002', 'c0000002-0000-0000-0000-000000000002', '87654321', '400530', 'GBP', 8920.00, 'ACTIVE'),
    ('a0000003-0000-0000-0000-000000000003', 'c0000003-0000-0000-0000-000000000003', '11223344', '601201', 'GBP', 75000.00, 'ACTIVE'),
    ('a0000004-0000-0000-0000-000000000004', 'c0000004-0000-0000-0000-000000000004', '99887766', '309089', 'GBP', 4200.75, 'ACTIVE');

-- 4. Merchants & Risk Ratings (Valid Hexadecimal UUIDs: e0000001...)
INSERT INTO merchants (id, merchant_code, merchant_name, mcc, category_name, risk_score_base)
VALUES
    ('e0000001-0000-0000-0000-000000000001', 'MERC-TESCO', 'Tesco Stores UK', '5411', 'Groceries', 5),
    ('e0000002-0000-0000-0000-000000000002', 'MERC-AMZN', 'Amazon UK Retail', '5311', 'Online Marketplace', 10),
    ('e0000003-0000-0000-0000-000000000003', 'MERC-TFL', 'Transport for London (TfL)', '4111', 'Public Transit', 2),
    ('e0000004-0000-0000-0000-000000000004', 'MERC-CURRYS', 'Currys PC World', '5732', 'Electronics', 25),
    ('e0000005-0000-0000-0000-000000000005', 'MERC-BET365', 'Bet365 Online', '7995', 'Gambling/Betting', 65),
    ('e0000006-0000-0000-0000-000000000006', 'MERC-BINANCE', 'Binance UK Crypto Exchange', '6051', 'Crypto/Quasi-Cash', 75),
    ('e0000007-0000-0000-0000-000000000007', 'MERC-WESTERNUNION', 'Western Union Wire', '4829', 'Wire Transfer', 80);

-- 5. Customer Trusted Devices
INSERT INTO devices (id, customer_id, device_fingerprint, device_type, ip_address, location_city, location_country, is_trusted)
VALUES
    ('d0000001-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001', 'iphone_15_pro_oliver', 'MOBILE_IOS', '82.132.224.12', 'London', 'GB', true),
    ('d0000002-0000-0000-0000-000000000002', 'c0000002-0000-0000-0000-000000000002', 'pixel_8_emma', 'MOBILE_ANDROID', '86.145.10.45', 'Oxford', 'GB', true);

-- 6. Initial Risk Profiles
INSERT INTO risk_profiles (customer_id, avg_transaction_amount_30d, tx_count_last_24h, last_known_ip, last_known_latitude, last_known_longitude, last_transaction_time, fraud_incident_count, overall_trust_score)
VALUES
    ('c0000001-0000-0000-0000-000000000001', 45.50, 2, '82.132.224.12', 51.5074, -0.1278, CURRENT_TIMESTAMP, 0, 95),
    ('c0000002-0000-0000-0000-000000000002', 32.00, 1, '86.145.10.45', 51.7520, -1.2577, CURRENT_TIMESTAMP, 0, 98),
    ('c0000003-0000-0000-0000-000000000003', 180.00, 3, '185.220.101.5', 51.5074, -0.1278, CURRENT_TIMESTAMP, 1, 80),
    ('c0000004-0000-0000-0000-000000000004', 1200.00, 7, '91.240.118.8', 52.4862, -1.8904, CURRENT_TIMESTAMP, 3, 50);
