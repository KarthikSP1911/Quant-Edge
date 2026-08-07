CREATE TABLE order_executions (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id),
    execution_price DECIMAL(15, 2) NOT NULL,
    executed_quantity INTEGER NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_executions_order ON order_executions (order_id);
