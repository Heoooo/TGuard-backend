-- Ensure long messages and error traces are not truncated
ALTER TABLE notification
    ALTER COLUMN message TYPE TEXT;

ALTER TABLE dlq_transaction_retry
    ALTER COLUMN error_message TYPE TEXT,
    ALTER COLUMN error_type TYPE TEXT;
