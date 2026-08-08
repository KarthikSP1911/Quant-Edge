# API Contract

Per CLAUDE.md's API Design Rules: **REST** for writes, **GraphQL** for reads, **SSE** for push.
Kafka is internal only — the frontend never touches it.

Each Phase 4 feature documents its own endpoints in its own section below. Only edit the section
for the feature you are building.

## Audit Log

Owner: `phase-4/audit-log-aop`

### GraphQL

```graphql
type AuditLogEntry {
  id: ID!
  action: String!
  entityType: String!
  entityId: String
  details: String
  ipAddress: String
  createdAt: String!
}

type AuditLogPage {
  content: [AuditLogEntry!]!
  totalElements: Int!
  totalPages: Int!
  page: Int!
  size: Int!
}

type Query {
  activityTimeline(
    action: String
    entityType: String
    startDate: String
    endDate: String
    page: Int = 0
    size: Int = 20
  ): AuditLogPage!
}
```

`activityTimeline` returns the authenticated user's own audit trail, newest first.

- `action` / `entityType` — exact-match filters (e.g. `BUY`, `SELL`, `PLACE_ORDER`,
  `CANCEL_ORDER`, `WATCHLIST_ADD`, `WATCHLIST_REMOVE`; `ORDER`, `WATCHLIST`).
- `startDate` / `endDate` — ISO-8601 instants (e.g. `2026-08-01T00:00:00Z`), inclusive.
- `details` is a JSON string of the audited method's non-user arguments; render it, don't dump it raw.

### Side effect (no REST/GraphQL surface)

State-changing actions (buy/sell, place/cancel order, watchlist add/remove) are annotated
`@Auditable` in the service layer. `AuditAspect` (Spring AOP) writes one `audit_logs` row per
successful call automatically — no endpoint triggers this directly.

## Stock Comparison

### `stockComparison` — GraphQL query (read)

Compares 2–3 stocks side by side: aligned fundamentals plus a shared-axis price history for the
normalized overlay chart.

```graphql
stockComparison(
  symbols: [String!]!
  interval: String = "1day"
  outputSize: Int = 90
): StockComparison!
```

**Arguments**

| Argument     | Type         | Required | Notes                                                                                   |
| ------------ | ------------ | -------- | --------------------------------------------------------------------------------------- |
| `symbols`    | `[String!]!` | yes      | 2–3 symbols. Trimmed, upper-cased and de-duplicated server-side before the count check. |
| `interval`   | `String`     | no       | Twelve Data interval (`5min`, `1h`, `1day`, `1week`). Defaults to `1day`.               |
| `outputSize` | `Int`        | no       | Bars requested per symbol. Defaults to `90`.                                            |

**Types**

```graphql
type StockComparison {
  entries: [ComparisonEntry!]!
}

type ComparisonEntry {
  company: Company!
  quote: Quote!
  fundamentals: Fundamentals!
  candles: [Candle!]!
}

type Fundamentals {
  marketCap: Float
  peRatio: Float
  fiftyTwoWeekHigh: Float
  fiftyTwoWeekLow: Float
}
```

`Company`, `Quote` and `Candle` are the existing Phase 2 types — unchanged.

**Guarantees**

- `entries` preserves the order the symbols were requested in — one entry per symbol.
- Every entry's `candles` share an identical datetime axis: same length, same datetimes, sorted
  oldest-first. The backend intersects the per-symbol series, so a bar one symbol is missing (a
  halt, a later listing date, a provider gap) is dropped from all of them rather than silently
  shifting one series against the others. If the stocks share no bars, every `candles` list is
  empty.
- All four `Fundamentals` fields are nullable. The comparison table renders the same rows for every
  stock, so a metric the provider has no data for is `null` (rendered as an em dash), never `0`.

**Errors**

| Condition                                    | Error                                     |
| -------------------------------------------- | ----------------------------------------- |
| Fewer than 2 or more than 3 distinct symbols | `InvalidComparisonRequestException` (400) |
| A symbol is not in the `companies` table     | `CompanyNotFoundException` (404)          |

**Caching / rate limits**

Cache-first, reusing the Phase 2 read paths. A warm cache serves a comparison with **zero** external
calls. On a cold cache the cost is exactly one call per symbol per upstream:

| Data                          | Cache               | TTL    | Source on miss             |
| ----------------------------- | ------------------- | ------ | -------------------------- |
| Quote                         | `PriceCache`        | 15 min | Finnhub `/quote`           |
| Candles                       | `ChartCache`        | 60 min | Twelve Data `/time_series` |
| Market cap, P/E, 52w high/low | `FundamentalsCache` | 24 h   | Finnhub `/stock/metric`    |

Company reference data comes from Postgres and never hits an external API.

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
