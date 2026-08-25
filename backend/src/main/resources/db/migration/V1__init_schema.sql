-- =========================================================================
-- Real-Time Banking Fraud Detection & Risk Intelligence Schema DDL
-- Compatible with PostgreSQL 16
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Customers Table (UK Banking Customers)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_number VARCHAR(32) UNIQUE NOT NULL,
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    email VARCHAR(128) UNIQUE NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    home_city VARCHAR(64) DEFAULT 'London',
    home_country VARCHAR(2) DEFAULT 'GB',
    risk_tier VARCHAR(16) DEFAULT 'LOW',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bank Accounts Table (UK Sort Code & Account Number)
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    account_number VARCHAR(8) UNIQUE NOT NULL,
    sort_code VARCHAR(6) NOT NULL,
    currency VARCHAR(3) DEFAULT 'GBP',
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Merchants Table (with MCC codes & risk weighting)
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_code VARCHAR(64) UNIQUE NOT NULL,
    merchant_name VARCHAR(128) NOT NULL,
    mcc VARCHAR(4) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    risk_score_base INT DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Customer Devices Table
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(128) NOT NULL,
    device_type VARCHAR(32) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    location_city VARCHAR(64),
    location_country VARCHAR(2) DEFAULT 'GB',
    is_trusted BOOLEAN DEFAULT TRUE,
    first_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_device UNIQUE(customer_id, device_fingerprint)
);

-- 5. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(128) UNIQUE NOT NULL,
    source_account_id UUID NOT NULL REFERENCES accounts(id),
    destination_account_number VARCHAR(34) NOT NULL,
    merchant_id UUID REFERENCES merchants(id),
    device_id UUID REFERENCES devices(id),
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'GBP',
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    risk_score INT,
    decision_reason TEXT,
    ip_address VARCHAR(45),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Fraud Alerts Table
CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    rule_score INT NOT NULL,
    ml_score INT NOT NULL,
    composite_risk_score INT NOT NULL,
    triggered_rules TEXT,
    ml_feature_contributions TEXT,
    status VARCHAR(24) DEFAULT 'PENDING_REVIEW',
    assigned_analyst VARCHAR(64),
    analyst_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- 7. Risk Profiles Table
CREATE TABLE IF NOT EXISTS risk_profiles (
    customer_id UUID PRIMARY KEY REFERENCES customers(id) ON DELETE CASCADE,
    avg_transaction_amount_30d NUMERIC(15, 2) DEFAULT 0.00,
    tx_count_last_24h INT DEFAULT 0,
    last_known_ip VARCHAR(45),
    last_known_latitude DOUBLE PRECISION,
    last_known_longitude DOUBLE PRECISION,
    last_transaction_time TIMESTAMP WITH TIME ZONE,
    fraud_incident_count INT DEFAULT 0,
    overall_trust_score INT DEFAULT 95,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Audit Logs Table (Append-Only)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    actor_username VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64),
    ip_address VARCHAR(45),
    before_state TEXT,
    after_state TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. System Users & Authentication Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(128) UNIQUE NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL, -- ROLE_CUSTOMER, ROLE_FRAUD_ANALYST, ROLE_ADMIN
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Performance & Query Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_tx_source_account ON transactions(source_account_id);
CREATE INDEX IF NOT EXISTS idx_tx_created_at ON transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tx_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_tx_id ON fraud_alerts(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_cust_id ON fraud_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_correlation ON audit_logs(correlation_id);
