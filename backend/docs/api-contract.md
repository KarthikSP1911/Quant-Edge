# QuantEdge API Contract

This file is the source of truth for the REST + GraphQL contract between the QuantEdge
backend and frontend. Update it in the same commit as any endpoint/schema change.

- REST is used for writes: auth, buy/sell, orders, exports, chat, agent triggers.
- GraphQL is used for reads: company data, portfolio, watchlist, transactions, dashboard.
- SSE is used for push: order fills, agent reasoning trace.

## Conventions

- All REST responses are JSON. Errors follow the shape produced by `GlobalExceptionHandler`.
- All timestamps are ISO-8601 UTC.
- Money fields are decimal strings (never floats) to avoid precision loss in transit.

## REST Endpoints

See `AuthController` for Phase 1 auth endpoints (login, register, refresh, OAuth callback).

_(Phase 2 REST endpoints — buy/sell, watchlist writes — are added in later slices of this
phase and documented here as they land.)_

## GraphQL Schema

Schema file: `src/main/resources/graphql/schema.graphqls` (single central schema file).

_(No GraphQL queries exist yet. The `companies` query and `Company` type are added in
slice 5 — `phase-2/graphql-setup-and-company-queries`.)_

## Data Model — Company (Phase 2, slice 1)

Table: `companies` (see `V5__create_companies_table.sql`)

| Column      | Type         | Notes                 |
| ----------- | ------------ | --------------------- |
| id          | UUID         | primary key           |
| symbol      | VARCHAR(10)  | unique, e.g. `AAPL`   |
| name        | VARCHAR(255) |                       |
| sector      | VARCHAR(100) | indexed               |
| industry    | VARCHAR(150) |                       |
| description | TEXT         | nullable              |
| logo_url    | VARCHAR(500) | nullable              |
| exchange    | VARCHAR(50)  | e.g. `NASDAQ`, `NYSE` |
| created_at  | TIMESTAMPTZ  |                       |
| updated_at  | TIMESTAMPTZ  |                       |

Seeded with 20 real symbols across 7 sectors (`V6__seed_companies.sql`). No REST/GraphQL
surface exists for this table yet — entity + repository only. The `companies` GraphQL query
(filter by sector, search) lands in slice 5.
