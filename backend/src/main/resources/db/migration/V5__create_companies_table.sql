CREATE TABLE companies (
    id UUID PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    sector VARCHAR(100) NOT NULL,
    industry VARCHAR(150) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    exchange VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companies_sector ON companies (sector);
