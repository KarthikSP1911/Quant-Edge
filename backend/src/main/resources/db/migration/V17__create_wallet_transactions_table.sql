CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    stripe_session_id VARCHAR(255) NOT NULL UNIQUE,
    stripe_payment_intent_id VARCHAR(255),
    amount_usd_cents BIGINT NOT NULL,
    credits_awarded DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallet_transactions_user ON wallet_transactions (user_id);
