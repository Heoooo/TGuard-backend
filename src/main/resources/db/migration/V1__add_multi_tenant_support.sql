-- Base schema to enable multi-tenant isolation

-- Users ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    username VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(40) NOT NULL,
    role VARCHAR(40) NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_username_tenant'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uk_user_username_tenant UNIQUE (username, tenant_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_phone_tenant'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uk_user_phone_tenant UNIQUE (phone_number, tenant_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_tenant ON users (tenant_id);

-- Cards ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand VARCHAR(32) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    nickname VARCHAR(50)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_card_owner_brand_last4'
    ) THEN
        ALTER TABLE cards
            ADD CONSTRAINT uk_card_owner_brand_last4 UNIQUE (owner_id, brand, last4);
    END IF;
END $$;

-- Rules ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    rule_name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_rule_tenant_name'
    ) THEN
        ALTER TABLE rule
            ADD CONSTRAINT uk_rule_tenant_name UNIQUE (tenant_id, rule_name);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rule_tenant ON rule (tenant_id);

-- Transactions --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    location VARCHAR(255),
    device_info VARCHAR(255),
    channel VARCHAR(40) NOT NULL,
    transaction_time TIMESTAMP NOT NULL,
    status VARCHAR(40) NOT NULL,
    external_event_id VARCHAR(255),
    payment_key VARCHAR(255),
    order_id VARCHAR(255),
    currency VARCHAR(16),
    raw_payload TEXT
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tx_external_event') THEN
        ALTER TABLE transaction
            ADD CONSTRAINT uk_tx_external_event UNIQUE (tenant_id, external_event_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tx_payment_key') THEN
        ALTER TABLE transaction
            ADD CONSTRAINT uk_tx_payment_key UNIQUE (tenant_id, payment_key);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tx_order_id') THEN
        ALTER TABLE transaction
            ADD CONSTRAINT uk_tx_order_id UNIQUE (tenant_id, order_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_tx_tenant_time ON transaction (tenant_id, transaction_time);
CREATE INDEX IF NOT EXISTS idx_tx_user_time ON transaction (user_id, transaction_time);

-- Detection results ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS detection_result (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    transaction_id BIGINT NOT NULL REFERENCES transaction(id) ON DELETE CASCADE,
    rule_id BIGINT NOT NULL REFERENCES rule(id) ON DELETE CASCADE,
    suspicious BOOLEAN NOT NULL,
    risk_score INTEGER NOT NULL DEFAULT 0,
    probability DOUBLE PRECISION NOT NULL DEFAULT 0,
    reason TEXT NOT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_detection_tenant ON detection_result (tenant_id);

-- Notifications -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transaction_id BIGINT NOT NULL REFERENCES transaction(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notification_tenant ON notification (tenant_id);

-- Blocked users -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blocked_user (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Batch ingestion tables ----------------------------------------------------
CREATE TABLE IF NOT EXISTS batch_transaction_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    transaction_id BIGINT,
    user_id BIGINT NOT NULL,
    amount DOUBLE PRECISION,
    location VARCHAR(255),
    device_info VARCHAR(255),
    channel VARCHAR(40),
    transaction_time TIMESTAMP,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_batch_event_tenant_processed
    ON batch_transaction_event (tenant_id, processed);
CREATE INDEX IF NOT EXISTS idx_batch_event_tenant_time
    ON batch_transaction_event (tenant_id, transaction_time);

CREATE TABLE IF NOT EXISTS daily_transaction_report (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    report_date DATE NOT NULL,
    user_id BIGINT NOT NULL,
    transaction_count INTEGER NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL,
    average_amount DOUBLE PRECISION NOT NULL,
    dominant_channel VARCHAR(40),
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_daily_report_tenant_date'
    ) THEN
        ALTER TABLE daily_transaction_report
            ADD CONSTRAINT uk_daily_report_tenant_date UNIQUE (tenant_id, report_date);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_daily_report_tenant ON daily_transaction_report (tenant_id);

-- DLQ retry -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dlq_transaction_retry (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    transaction_id BIGINT,
    payload TEXT NOT NULL,
    error_message TEXT,
    error_type TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL,
    last_failed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_alerted_attempt INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dlq_retry_tenant ON dlq_transaction_retry (tenant_id);
CREATE INDEX IF NOT EXISTS idx_dlq_retry_next_retry ON dlq_transaction_retry (next_retry_at);
