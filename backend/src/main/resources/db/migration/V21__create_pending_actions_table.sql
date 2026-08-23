CREATE TABLE pending_actions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_run_id UUID REFERENCES agent_runs(id) ON DELETE SET NULL,
    source VARCHAR(20) NOT NULL,
    request_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_pending_actions_user_status ON pending_actions(user_id, status);
CREATE INDEX idx_pending_actions_agent_run_id ON pending_actions(agent_run_id);

-- Enforces one open proposal per user at a time, across both chat- and agent-originated proposals.
CREATE UNIQUE INDEX idx_pending_actions_one_pending_per_user
    ON pending_actions(user_id)
    WHERE status = 'PENDING';
