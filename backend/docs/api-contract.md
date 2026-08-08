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

### Order fill stream (Phase 3, part 3)

**`GET /api/orders/stream`** — Server-Sent Events stream of fill notifications for the
authenticated user. `EventSource` cannot set an `Authorization` header, so this endpoint alone
also accepts the access token as a query param: `GET /api/orders/stream?token=<accessToken>`
(`JwtAuthFilter` only honors the query param on this exact path — every other endpoint still
requires the `Authorization: Bearer` header). One connection is registered per tab/device; a user
with several open tabs gets the event pushed to each.

Event, name `order-fill`:

```json
{
  "executionId": "c2a4...",
  "orderId": "b3f1...",
  "userId": "9e21...",
  "symbol": "AAPL",
  "side": "BUY",
  "quantity": 10,
  "price": "181.42",
  "executedAt": "2026-08-08T09:05:12Z"
}
```

Only fills produced by the Part 2 Kafka matcher (`OrderMatcherService` → `executed-trades` →
`TradeExecutedConsumer`) are pushed here — synchronous market buy/sell already returns the fill
in the REST response, so it does not also publish to this stream. The emitter is held in-memory
per backend instance (`OrderSseRegistry`); it is not durable across a restart or shared across
multiple backend instances, which is acceptable at the current single-instance scale.

## GraphQL Schema

Schema file: `src/main/resources/graphql/schema.graphqls` (single central schema file).

### Orders (Phase 3, part 3)

```graphql
enum OrderStatus {
  PENDING
  OPEN
  FILLED
  PARTIALLY_FILLED
  CANCELLED
  REJECTED
  EXPIRED
}

enum OrderType {
  MARKET
  LIMIT
  STOP_LOSS
  STOP_LIMIT
}

type Order {
  id: ID!
  symbol: String!
  side: OrderSide!
  type: OrderType!
  status: OrderStatus!
  quantity: Int!
  filledQuantity: Int!
  limitPrice: Float
  stopPrice: Float
  createdAt: String!
  updatedAt: String!
  expiresAt: String
}

type Query {
  openOrders: [Order!]! # status in (PENDING, OPEN)
  filledOrders: [Order!]! # status in (FILLED, PARTIALLY_FILLED)
  orderHistory: [Order!]! # every order for the caller, newest first
}
```

`filledQuantity` is `quantity` when `status == FILLED`, otherwise `0` — the current matcher and
market-order paths always fill an order in full, so there is no partial-fill accounting yet
despite `PARTIALLY_FILLED` existing as a status value reserved for future use.

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

## Portfolio Time Machine (Phase 4)

Pure read, reconstructed from `order_executions` (there is no separate `transactions` table —
see the Dashboard section's note). No new schema.

```graphql
type TimeMachineHolding {
  company: Company!
  quantity: Int!
  averageCost: Float!
  priceAtDate: Float!
  marketValue: Float!
  gainLoss: Float!
  gainLossPercent: Float!
}

type TimeMachineDecision {
  symbol: String!
  quantity: Int!
  buyPrice: Float!
  sellPrice: Float!
  executedAt: String!
  realizedGainPercent: Float!
}

type TimeMachineResult {
  asOfDate: String!
  cashBalance: Float!
  holdings: [TimeMachineHolding!]!
  totalMarketValue: Float!
  totalAccountValue: Float!
  bestDecisions: [TimeMachineDecision!]!
  worstDecisions: [TimeMachineDecision!]!
}

type Query {
  portfolioTimeMachine(asOfDate: String!): TimeMachineResult!
}
```

- `asOfDate` is an ISO date (`yyyy-MM-dd`); `400` (`InvalidTimeMachineRequestException`) if it
  isn't parseable or is in the future.
- `holdings` / `cashBalance` are computed by replaying every `OrderExecution` up to and including
  `asOfDate` (`TransactionReplayer`), using the same weighted-average-cost math as the live
  portfolio (`TradeExecutionService`) — no separate FIFO-lot bookkeeping. `cashBalance` starts
  from the fixed `$10,000.00` new-user default (there is no deposit/withdrawal feature, so every
  user's starting cash is that constant) and applies each historical buy/sell's cash delta.
- `priceAtDate` is the closing price on `asOfDate` (or the most recent prior trading day),
  resolved cache-first via `HistoricalPriceService` / Twelve Data `time_series` — separate Redis
  cache slot (`chart:{symbol}:1day-history`, 24h TTL) from the short recent-window chart cache
  `StockDetailService` uses, since this lookup needs much deeper history.
- **Best/worst decision metric — realized gain % per closed lot**: for each SELL execution up to
  `asOfDate`, `realizedGainPercent = (sellPrice − averageCostAtSale) / averageCostAtSale × 100`,
  where `averageCostAtSale` is the position's running weighted-average cost immediately before
  that sale. Only SELL executions produce a ranked entry — open (unsold) positions have no
  realized outcome yet. `bestDecisions` / `worstDecisions` are each the top 5 by this metric,
  descending / ascending.

### Chat Agent (Phase 5, part 1)

**POST /api/chat** � Send a message to the AI Chat agent.
Request:
`json
{
  "message": "What is the price of NVDA?"
}
`
Response 200:
`json
{
  "response": "The current real-time price of NVDA is 105.00."
}
`
_Note: The Chat Agent uses 9 specific tool functions to fulfill user requests, including placing orders, getting quotes, adding to the watchlist, etc. The chat history is saved to the DB._

### Research Agent (Phase 5, part 2)

**POST /api/v1/agent/research/{symbol}** � Trigger an autonomous 5-step research task for a specific symbol.
Response 200:
`json
{
  "sessionId": "UUID-of-trace-session"
}
`

**GET /api/v1/agent/trace/{sessionId}?token=<accessToken>** � Server-Sent Events stream for the reasoning trace of the triggered research agent.
Events are of type race, and have JSON data containing step and message properties representing the agent's progress.
