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

### Orders — place/cancel (Phase 3, part 1)

`OrderController` (`/api/orders`). Market orders (`POST /api/orders/buy|sell`) execute
synchronously and are unaffected — see Phase 2. `LIMIT`/`STOP_LOSS`/`STOP_LIMIT` orders rest on
the book as `OPEN` until the Part 2 matcher triggers them against a synced price; this endpoint
does not execute anything.

**`POST /api/orders`** — place a limit/stop order.

Request:

```json
{
  "symbol": "AAPL",
  "side": "BUY",
  "type": "LIMIT",
  "quantity": 10,
  "limitPrice": "180.00",
  "stopPrice": null,
  "timeInForce": "GTC"
}
```

- `type`: `LIMIT` | `STOP_LOSS` | `STOP_LIMIT` (not `MARKET` — use `/buy`/`/sell` for that).
- `limitPrice` required for `LIMIT`/`STOP_LIMIT`; `stopPrice` required for `STOP_LOSS`/`STOP_LIMIT`.
- `timeInForce`: `DAY` (expires at 23:59:59 UTC same day) | `GTC` (no expiry).

Response `200`:

```json
{
  "id": "b3f1...",
  "symbol": "AAPL",
  "side": "BUY",
  "type": "LIMIT",
  "quantity": 10,
  "limitPrice": "180.00",
  "stopPrice": null,
  "timeInForce": "GTC",
  "status": "OPEN",
  "expiresAt": null,
  "createdAt": "2026-08-08T09:00:00Z"
}
```

Errors: `400` if `type` is `MARKET`, if the required price field for the type is missing, or if
validation fails (`symbol`/`side`/`type`/`timeInForce` required, `quantity >= 1`). `404` if the
symbol is unknown.

**`POST /api/orders/{orderId}/cancel`** — cancels an `OPEN` order owned by the caller. Returns the
same shape as above with `status: "CANCELLED"`. `404` if the order doesn't exist or isn't the
caller's; `400` if the order isn't currently `OPEN` (already filled/cancelled/expired).

### Orders — matching, execution, expiry (Phase 3, part 2)

`status` on a placed order is no longer static after the initial `OPEN` response - it now
transitions asynchronously:

- `OPEN` → `FILLED`: the Kafka matcher (consuming `stock-prices`) triggered the order per the
  agreed rules and filled it. Fetch the order again (or via GraphQL reads) to see the terminal
  state and the resulting `OrderExecution`.
- `OPEN` → `REJECTED`: the order triggered, but the fill failed at execution time (insufficient
  balance/shares - funds aren't reserved at placement, only checked at fill time).
- `OPEN` → `EXPIRED`: a `DAY` order's `expires_at` passed before it triggered (sweep runs every
  `order-expiry.fixed-rate-ms`, default 60s). `GTC` orders never expire this way.

`PARTIALLY_FILLED` is defined on `OrderStatus` but intentionally unused - there is no order book,
only a single reference price, so fills are always all-or-nothing.

Balance/share reservation at placement time is still not implemented - funds are only checked when
the matcher attempts the fill, per the above.

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
