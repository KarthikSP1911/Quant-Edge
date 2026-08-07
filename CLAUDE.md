# CLAUDE.md

## Project

**QuantEdge** — AI-powered stock research and simulated trading platform.
Tagline: "AI-powered stock research, quantified"
Building from scratch. No existing codebase to migrate or reference.

## Repo Layout

```
/backend    → Spring Boot 3.x (Java 17+)
/frontend   → Next.js (App Router, TypeScript, Tailwind)
/ml         → FastAPI + Python (optional, Phase 7)
docker-compose.yml at root
```

## Tech Stack

- **Backend:** Spring Boot 3.x, Java 17+, Maven
- **Frontend:** Next.js App Router, TypeScript, Tailwind, shadcn/ui
- **DB:** PostgreSQL 15+ with Flyway migrations
- **Cache:** Redis 7+
- **Messaging:** Apache Kafka (KRaft mode, no Zookeeper)
- **GenAI:** Groq via Spring AI (OpenAI-compatible client)
- **Charts:** TradingView lightweight-charts
- **Testing:** JUnit 5, Mockito, Testcontainers
- **DevOps:** Docker Compose

## API Design Rules

- **REST** for writes: auth, buy/sell, place/cancel orders, export PDF/CSV, send chat, trigger agent
- **GraphQL** for reads: company list/detail, portfolio, watchlist, transactions, dashboard, orders, audit log, comparison, timeline, chat history, research notes
- **SSE** for push: order fill notifications, agent reasoning trace
- **Kafka** is internal only — the frontend never touches it
- Never add a REST read endpoint where a GraphQL query belongs, and vice versa

## Data Strategy

- **PostgreSQL** — all permanent data
- **Redis** — prices (15min TTL), charts (15–60min), news (1hr), indicators (24hr), profiles (24hr)
- Cache-first: ~90% of page loads must hit Redis/Postgres only. External APIs are called on cache miss only.
- External API rate limits are hard constraints: Finnhub 60/min, Twelve Data 800/day, Alpha Vantage 25/day, Groq 30/min

## Database — 11 tables

users, companies, portfolios, transactions, orders, order_executions, watchlists, audit_logs, research_notes, alerts, chat_history

All schema changes go through Flyway migrations. Never hand-edit a migration that has already been applied — write a new one.

## Branding / Design Tokens

```
Font: Inter (400, 500, 600) | Theme: light
Page bg        #FFFFFF
Card bg        #F8FAFC
Sidebar/hover  #F1F5F9
Border         #E2E8F0
Text primary   #0F172A
Text secondary #64748B
Text muted     #94A3B8
Accent blue    #2563EB
Accent light   #DBEAFE
Profit/up      #16A34A
Loss/down      #DC2626
Warning        #F59E0B
```
Logo: "Quant" in #0F172A + "Edge" in #2563EB. No tagline under the logo.

## Build Phases

1. Auth + project setup (7 features)
2. Core CRUD — companies, portfolio, market orders, dashboard, GraphQL reads (13)
3. Kafka + order matching engine — limit/stop-loss/stop-limit, SSE (16)
4. Standout features — audit log + AOP, comparison, time machine, exports (18)
5. Testing + DevOps — 80%+ coverage, Testcontainers, Actuator (9)
6. GenAI + research agent — Spring AI, 9 tools, 5-step agent, SSE trace (13)
7. ML optional — FastAPI, FinBERT, price prediction (8)

**Hard rule:** do not start a phase until the previous one is fully working and demo-able.

## Git Workflow — follow this strictly

- `main` is always green and demo-able. Never commit directly to `main`.
- One branch per feature: `phase-<n>/<short-kebab-description>`
  Examples: `phase-1/jwt-auth`, `phase-3/order-matcher-consumer`
- **Commit frequently** — after every logically complete unit of work, not at the end of a feature. A passing test, a new entity, a working endpoint, a migration: each is its own commit.
- Conventional commit messages: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`
  Example: `feat(auth): add refresh token rotation`
- Never bundle unrelated changes into one commit.
- Run the build and tests before every commit. Do not commit broken code.
- When a feature is done and verified, merge to `main` and delete the branch.
- Tell me the branch name before you start work on it.

## Working Agreement

- Ask before installing a new dependency that isn't in the planned stack.
- Never commit secrets. All keys go in `.env`, which is gitignored. Keep `.env.example` updated.
- Prefer editing existing files over creating new ones.
- When something in this file becomes stale, update CLAUDE.md as part of the same commit.
