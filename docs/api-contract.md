# API Contract

<!-- Each Phase 4 feature owns one section below. Only edit your own section. -->

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
