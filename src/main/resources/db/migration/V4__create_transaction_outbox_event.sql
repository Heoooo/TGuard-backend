CREATE TABLE IF NOT EXISTS transaction_outbox_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    transaction_id BIGINT,
    topic VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_tx_outbox_pending
    ON transaction_outbox_event (published, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_tx_outbox_tenant
    ON transaction_outbox_event (tenant_id);
