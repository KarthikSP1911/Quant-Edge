CREATE TABLE watchlists (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    company_id UUID NOT NULL REFERENCES companies (id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, company_id)
);

CREATE INDEX idx_watchlists_user ON watchlists (user_id);
