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

## Database — 12 tables

users, companies, portfolios, transactions, orders, order_executions, watchlists, audit_logs, research_notes, alerts, chat_history, wallet_transactions

All schema changes go through Flyway migrations. Never hand-edit a migration that has already been applied — write a new one.

`wallet_transactions` backs the Razorpay (Test Mode) wallet top-up feature (USD via Razorpay Checkout.js → virtual `users.balance` credits, at a fixed $1 = 10 credits rate; no real money is ever charged). The backend creates a Razorpay Order server-side, the frontend opens the Checkout.js modal client-side, and a JWT-authenticated verify-payment endpoint checks the HMAC-SHA256 payment signature before crediting — a `payment.captured` webhook (`X-Razorpay-Signature`-verified) backs that up idempotently in case the browser closes first. It lives outside the 7-phase build plan below — it was added on its own `feature/stripe-wallet-topup` branch rather than a `phase-<n>/*` one, and commits use the `wallet` commitlint scope.

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
- When a feature is done and verified, merge to `main` and delete the branch.
- Tell me the branch name before you start work on it.

## Tooling

Repo-wide git hooks and linting are enforced via **Lefthook**, installed once at the repo
root (not per-package).

### Setup (run once after clone)

```bash
npm install                              # root deps: lefthook, commitlint, prettier
                                          # postinstall auto-runs `lefthook install`
cd frontend && npm install
cd ../backend && ./mvnw -q -DskipTests install
```

If npm blocks lefthook's install script (`npm warn allow-scripts`), approve it manually,
then re-run `lefthook install`:

```bash
npm approve-scripts lefthook
npx lefthook install
```

### Git hooks (`lefthook.yml`, root)

- **pre-commit** (parallel, staged-files-only):
  - `frontend` — `eslint --fix` + `prettier --write` on staged `frontend/**/*.{ts,tsx,js,jsx}`
  - `backend` — `./mvnw spotless:apply` on staged `backend/**/*.java` (applies to the whole
    Maven module, not just the staged files — Spotless's Maven plugin has no clean per-file
    apply mode, but it's fast and idempotent so this is a deliberate tradeoff, not an oversight)
  - `general` — `prettier --write` on root-level `*.{json,md,yml,yaml}`
  - Any file a hook reformats is shown in the commit output and re-staged automatically
    (`stage_fixed: true`) — nothing is fixed silently off-screen.
- **commit-msg** — runs `commitlint` against the commit message (see Commit Convention below).
- **pre-push** (parallel):
  - `backend-test` — `./mvnw test` (fast unit tests only; the full Testcontainers suite from
    Phase 6 is not run here)
  - `frontend-check` — `npm run lint && npm run typecheck`

### Commit convention (`commitlint.config.js`, root)

Extends `@commitlint/config-conventional`. Enforced on every commit:

- **Type** (required): `feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `build`, `ci`
- **Scope** (required, not freeform): `auth`, `portfolio`, `orders`, `kafka`, `graphql`,
  `agent`, `ui`, `db`, `docker`, `tooling`
  (`tooling` was added beyond the original domain list for repo-wide/meta changes — CI config,
  lint rules, build scripts — that don't belong to a single feature domain)
- **Header max length**: 72 characters

Example: `feat(orders): add stop-limit order validation`

### Frontend — `/frontend`

- **ESLint** (`eslint.config.mjs`): `next/core-web-vitals` + `@typescript-eslint` recommended +
  `eslint-config-prettier` (disables formatting rules so ESLint and Prettier never fight).
  `no-unused-vars`, `no-floating-promises`, and `no-explicit-any` are all set to **error**.
  `no-floating-promises` requires type info, so `.ts`/`.tsx` files run with
  `parserOptions.projectService` enabled.
- **Prettier** (`.prettierrc.json`): no semicolons, single quotes, trailing commas, 100
  print width.
- Scripts: `npm run lint`, `npm run typecheck` (`tsc --noEmit`).

### Backend — `/backend`

- **Spotless** (`pom.xml`), formatter: **palantir-java-format** (4-space indent, matches the
  `.editorconfig` Java convention). Bound to `mvn verify` via `spotless:check` — the build
  **fails** on unformatted code, not just warns. Run `./mvnw spotless:apply` to fix locally.
- **Checkstyle** (`backend/checkstyle.xml`): flags unused imports, redundant imports, and
  enforces import ordering (static → java/javax → third-party, alphabetized within groups).
  Bound to `mvn verify` via `checkstyle:check`, `failOnViolation=true`.
- A `test`-scoped H2 profile (`backend/src/test/resources/application.properties`) backs the
  fast `mvn test` run used by pre-push, so `mvn test` doesn't require a live Postgres instance.
  Full-schema integration coverage against real Postgres still happens via Testcontainers in
  Phase 6.

### Root-level

- **`.editorconfig`** — 2-space indent for JS/TS/JSON/YAML/CSS/HTML, 4-space for Java, UTF-8,
  LF line endings, trim trailing whitespace (Markdown excluded, since trailing double-spaces
  are a valid hard-break there).
- **`.gitattributes`** — normalizes all text files to LF on checkout; `.bat`/`.cmd` keep CRLF.

## Working Agreement

- Ask before installing a new dependency that isn't in the planned stack.
- Never commit secrets. All keys go in `.env`, which is gitignored. Keep `.env.example` updated.
- Prefer editing existing files over creating new ones.
- When something in this file becomes stale, update CLAUDE.md as part of the same commit.
