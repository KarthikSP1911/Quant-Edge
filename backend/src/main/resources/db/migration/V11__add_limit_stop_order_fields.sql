ALTER TABLE orders
    ADD COLUMN limit_price DECIMAL(15, 2),
    ADD COLUMN stop_price DECIMAL(15, 2),
    ADD COLUMN time_in_force VARCHAR(10),
    ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN idempotency_key UUID;

-- Postgres unique constraints allow multiple NULLs, so existing MARKET orders
-- (idempotency_key left null - they execute synchronously, no matcher redelivery to guard against)
-- are unaffected.
ALTER TABLE orders ADD CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key);
