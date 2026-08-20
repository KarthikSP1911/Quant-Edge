CREATE TABLE agent_steps (
    id UUID PRIMARY KEY,
    agent_run_id UUID NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    phase VARCHAR(30) NOT NULL,
    tool_name VARCHAR(100),
    tool_input TEXT,
    tool_output TEXT,
    reasoning TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_steps_agent_run_id ON agent_steps(agent_run_id);
