CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    goal TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    max_steps INTEGER NOT NULL,
    step_count INTEGER NOT NULL DEFAULT 0,
    final_report_id UUID REFERENCES research_notes(id) ON DELETE SET NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_agent_runs_user_id ON agent_runs(user_id);
CREATE INDEX idx_agent_runs_created_at ON agent_runs(created_at);
