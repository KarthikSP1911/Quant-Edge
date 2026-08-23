# CLAUDE.md

## Project

**QuantEdge** — AI-powered stock research and simulated trading platform.
Tagline: "AI-powered stock research, quantified"
Building from scratch. No existing codebase to migrate or reference.

## Tech Stack

- **Backend:** Spring Boot 3.x, Java 17+, Maven
- **Frontend:** Next.js App Router, TypeScript, Tailwind, shadcn/ui
- **DB:** PostgreSQL 15+ with Flyway migrations
- **Cache:** Redis 7+
- **Messaging:** Apache Kafka (KRaft mode, no Zookeeper)
- **GenAI:** Groq via Spring AI (openai/gpt-oss-120b, OpenAI-compatible client)
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

## Database — 14 tables

users, companies, portfolios, transactions, orders, order_executions, watchlists, audit_logs, research_notes, alerts, chat_history, wallet_transactions, agent_runs, agent_steps

All schema changes go through Flyway migrations. Never hand-edit a migration that has already been applied — write a new one.

`wallet_transactions` backs the Razorpay (Test Mode) wallet top-up feature (USD via Razorpay Checkout.js → virtual `users.balance` credits, at a fixed $1 = 10 credits rate; no real money is ever charged). The backend creates a Razorpay Order server-side, the frontend opens the Checkout.js modal client-side, and a JWT-authenticated verify-payment endpoint checks the HMAC-SHA256 payment signature before crediting — a `payment.captured` webhook (`X-Razorpay-Signature`-verified) backs that up idempotently in case the browser closes first. `RAZORPAY_WEBHOOK_SECRET` is optional — Test Mode only hands out a key ID and key secret up front, and getting a webhook secret requires a public HTTPS URL for Razorpay to call; without it, the webhook endpoint just refuses requests (503) instead of skipping verification, and the primary verify-payment flow still works standalone. It lives outside the 7-phase build plan below — it was added on its own `feature/stripe-wallet-topup` branch rather than a `phase-<n>/*` one, and commits use the `wallet` commitlint scope.

`agent_runs`/`agent_steps` back the research agent's plan/act/observe loop (`ResearchAgentOrchestrator`) — durable task state and short/long-term memory for each run, replacing the old in-memory-only SSE trace as the source of truth for what the agent did. See README's "AI / Agentic Architecture" section for the full design.

## Branding / Design Tokens

```
Font: Inter (400, 500, 600) | Theme: light
Page bg        #F8FAFC
Card bg        #FFFFFF
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
5. GenAI + research agent — Spring AI, 9 tools, 5-step agent, SSE trace (13)
6. Testing + DevOps — 80%+ coverage, Testcontainers, Actuator (9)
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
- When a feature is done and verified, merge to `main` and delete the branch — ask permission
  before the delete step (see Working Agreement).
- Tell me the branch name before you start work on it.

## Tooling

Repo-wide git hooks and linting are enforced via **Lefthook**, installed once at the repo
root (not per-package). See `.claude/skills/setup/SKILL.md` for first-time setup steps.

Git hooks are defined in `lefthook.yml`; commit messages are checked by `commitlint.config.js`
against `@commitlint/config-conventional` (`tooling` was added to the scope list beyond the
original domain list, for repo-wide/meta changes — CI config, lint rules, build scripts — that
don't belong to a single feature domain). Frontend lint/format rules are in
`frontend/eslint.config.mjs` and `frontend/.prettierrc.json`; backend formatting/style rules are
in `pom.xml` (Spotless) and `backend/checkstyle.xml`. Root-level whitespace/line-ending rules are
in `.editorconfig` and `.gitattributes`.

A `test`-scoped H2 profile (`backend/src/test/resources/application.properties`) backs the
fast `mvn test` run used by pre-push, so `mvn test` doesn't require a live Postgres instance.
Full-schema integration coverage against real Postgres still happens via Testcontainers in
Phase 6.

## Working Agreement

- Ask before installing a new dependency that isn't in the planned stack.
- Ask before deleting anything — branches (local or remote), files, database rows/tables,
  migrations, or force-pushing. This applies even after a merge is verified green.
- Never commit secrets. All keys go in `.env`, which is gitignored. Keep `.env.example` updated.
- Prefer editing existing files over creating new ones.
- When something in this file becomes stale, update CLAUDE.md as part of the same commit.
