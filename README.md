# Quant Edge

<p align="center">
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white" alt="Next.js" style="margin: 4px;" />
  <img src="https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" style="margin: 4px;" />
  <img src="https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" style="margin: 4px;" />
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" style="margin: 4px;" />
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" style="margin: 4px;" />
  <img src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white" alt="Kafka" style="margin: 4px;" />
  <img src="https://img.shields.io/badge/Qdrant-000000?style=for-the-badge&logo=qdrant&logoColor=white" alt="Qdrant" style="margin: 4px;" />
  <img src="https://img.shields.io/badge/Groq-F55036?style=for-the-badge&logo=openai&logoColor=white" alt="Groq" style="margin: 4px;" />
</p>

<p align="center"><i>AI-powered stock research and simulated trading — quantified.</i></p>

## 🚀 Overview

**Quant Edge** is an AI-powered stock research and simulated trading platform. It combines live market data, a limit/stop order matching engine, and two purpose-built LLM surfaces — a tool-calling chat assistant and a multi-step research agent with its own plan/act/observe loop — to help a user research a symbol, place simulated trades, and track a virtual portfolio. Reads go through GraphQL, writes go through REST, and fills/agent progress stream over SSE; Kafka carries order-matching events internally and is never touched by the frontend.

## ✨ Key Features

- **Simulated trading & order matching** — market, limit, stop-loss, and stop-limit orders against a Kafka-backed matching engine, with live fill notifications pushed over SSE (`OrderStreamController`).
- **AI chat assistant** — a single-turn, tool-calling assistant (`ChatService`) over the user's portfolio, watchlist, order history, and RAG search; it can _stage_ a trade proposal but never execute one — only an explicit Accept click on the `PendingOrderCard` confirms it.
- **Research agent** — a multi-step plan/act/observe agent (`ResearchAgentOrchestrator`) that decides for itself which read-only tools to call (company profile, news, indicators, knowledge-base RAG search) for a given symbol, with guardrails on step count, per-tool timeout/retries, and wall-clock budget, and a live SSE reasoning trace.
- **Retrieval-augmented knowledge base** — daily Finnhub news and company data embedded locally (ONNX MiniLM) and stored in Qdrant, queried by both AI surfaces as a normal tool call.
- **Portfolio tools** — dashboard, watchlist with live quotes, stock comparison, an audit log (via AOP), a "time machine" portfolio replay, and PDF/CSV exports (iText7 / OpenCSV).
- **Simulated wallet top-up** — Razorpay Checkout (Test Mode) funds a virtual balance at a fixed $1 = 10 credits rate; no real money ever moves. HMAC-verified server-side, backed by an idempotent webhook.
- **Secure auth** — Spring Security with Google OAuth2 and JWT access/refresh tokens.
- **Cache-first data layer** — Redis-backed caching (prices, charts, news, indicators, profiles) in front of Finnhub / Twelve Data / Alpha Vantage, so ~90% of page loads never hit an external API.

## 🛠️ Prerequisites

- **Java 21**
- **Node.js 20+**
- **Docker & Docker Compose** (for spinning up local database, cache, and messaging infrastructure)

## 🚀 Getting Started

### 1. Start Infrastructure Services

Use Docker Compose to spin up the required local infrastructure (PostgreSQL, Redis via Serverless HTTP, Zookeeper, and Kafka):

```bash
docker-compose --profile local up -d
```

### 2. Environment Configuration

Create a `.env` file in the root directory (use `.env.example` as a template). You'll need to configure key variables such as:

- **Database:** `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- **Redis (Upstash/Local):** `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN`
- **External APIs:** `FINNHUB_API_KEY`, `TWELVE_DATA_API_KEY`, `ALPHA_VANTAGE_API_KEY`, `GROQ_API_KEY` (Groq, via Spring AI's OpenAI-compatible client — see [AI / Agentic Architecture](#-ai--agentic-architecture))
- **Vector Store:** `QDRANT_URL`, `QDRANT_API_KEY`
- **Auth:** `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_ACCESS_SECRET`

### 3. Repo Layout

```
/backend    → Spring Boot 3.x (Java 21), Maven — REST + GraphQL API, Kafka producer/consumer,
              Flyway migrations, Spring AI (Groq) chat + research agent
/frontend   → Next.js (App Router, TypeScript, Tailwind, shadcn/ui)
docker-compose.yml at root — Postgres, Redis (+ Upstash-REST shim), Zookeeper, Kafka
```

### 4. API Surface

| Style       | Used for | Examples                                                                                                                                      |
| ----------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| **REST**    | writes   | auth, buy/sell, place/cancel orders, wallet top-up, export PDF/CSV, send chat, trigger research agent                                         |
| **GraphQL** | reads    | company list/detail, portfolio, watchlist, transactions, dashboard, orders, audit log, comparison, time machine, chat history, research notes |
| **SSE**     | push     | order fill notifications (`OrderStreamController`), research agent reasoning trace (`GET /api/v1/agent/trace/{sessionId}`)                    |

Kafka is internal only — it carries order-matching events between the backend's own producer and consumer and is never exposed to the frontend.

### 5. Run the Application

#### Windows (Quick Start)

A convenient startup script is provided for Windows environments that will launch both the frontend and backend simultaneously:

```bat
start.bat
```

#### Manual Startup

- **Backend**:

  ```bash
  cd backend
  ./mvnw spring-boot:run
  ```

  _The backend API will be available at `http://localhost:8080`_

- **Frontend**:
  ```bash
  cd frontend
  npm install
  npm run dev
  ```
  _The frontend UI will be available at `http://localhost:3000`_

---

## ✨ Component Interaction

```mermaid
flowchart TB

    UI["Next.js UI<br/>(Frontend)"]
    API["Spring Boot API<br/>(Logic Gateway)"]
    Kafka["Kafka<br/>(Message Queue)"]
    DB["PostgreSQL<br/>(Data JPA / Flyway)"]
    Qdrant["Qdrant<br/>(Vector Store)"]
    LLM["Groq<br/>(Spring AI)"]

    UI <--> API
    API <--> Kafka
    API <--> DB
    API <--> Qdrant
    API <--> LLM

    %% Styling
    style UI fill:#D9EEF7,stroke:#000,stroke-width:2px,color:#000
    style API fill:#DCEBC3,stroke:#000,stroke-width:2px,color:#000
    style Kafka fill:#FF9900,stroke:#000,stroke-width:2px,color:#000
    style DB fill:#316192,stroke:#000,stroke-width:2px,color:#FFF
    style Qdrant fill:#F8E7A6,stroke:#000,stroke-width:2px,color:#000
    style LLM fill:#DDB7ED,stroke:#000,stroke-width:2px,color:#000
```

## ✨ Architecture Design

```mermaid
flowchart LR

    U[User / Client]

    subgraph F["Next.js Frontend App"]
        UI[UI Components & Dashboards]
        Charts[Lightweight Charts]
    end

    subgraph B["Spring Boot Backend"]
        direction TB
        SM[OAuth2 & JWT Security]
        API[GraphQL & REST APIs]
        SAI[Spring AI Service]
        EXP[PDF / CSV Exporter]
        KFP[Kafka Producer]
        KFC[Kafka Consumer]

        SM --> API
        API --> SAI
        API --> EXP
        API --> KFP
        KFC --> SAI
    end

    subgraph D["Data Layer"]
        P[(PostgreSQL)]
        Q[(Qdrant Vector Store)]
        K[(Kafka Event Streaming)]
    end

    subgraph External["External Services"]
        OA[Groq API]
    end

    U -->|HTTPS / WSS| F
    F -->|GraphQL / REST| B

    B -->|Read / Write| P
    B -->|Semantic Search| Q
    KFP -->|Publish Events| K
    K -->|Consume Events| KFC

    SAI -->|API Calls| OA
```

## ✨ AI / Agentic Architecture

QuantEdge has two distinct AI surfaces built on Spring AI + Groq (OpenAI-compatible client) +
Qdrant. They are deliberately separate rather than one shared code path:

- **Chat assistant** (`ChatService`, `ChatController` — `POST /api/v1/chat`): a single-turn
  request/response over the user's full tool surface (`ChatTools`) — portfolio/dashboard/
  watchlist reads, watchlist mutation, order history, RAG search, and staging a trade proposal.
  Spring AI's built-in tool-calling loop handles this turn's back-and-forth internally.
- **Research agent** (`ResearchAgentOrchestrator`, `ResearchAgentController` — `POST
/api/v1/agent/research/{symbol}` + SSE trace at `GET /api/v1/agent/trace/{sessionId}`): a
  multi-step, explicitly-looped agent that plans its own research strategy for a stock symbol
  instead of following a fixed script.

### From a hardcoded pipeline to a plan/act/observe loop

The research agent used to run five fixed steps in a fixed order — fetch profile, fetch news,
fetch indicators, one LLM synthesis call, save — regardless of whether each source was actually
useful for the symbol in question. `ResearchAgentOrchestrator` replaces that with an explicit
loop: the model decides which of its tools to call, in what order, how many times, and when it
has enough information to stop.

```mermaid
flowchart TB
    Start([Goal: research a symbol]) --> Plan[Plan\nmodel decides next action]
    Plan -->|no tool calls| Final[Final response\nMarkdown report]
    Plan -->|tool call(s)| Execute[Execute\nResearchAgentTools, via RetryingToolExecutor]
    Execute --> Observe[Observe\nresult or failure fed back as a message]
    Observe -->|failure| Replan[Replan\nmodel reacts to the gap]
    Observe -->|success| Plan
    Replan --> Plan
    Final --> Save[Deterministic save\nResearchNote, not an LLM tool]
    Save --> Done([complete])

    Plan -.max steps exceeded.-> Forced[Forced final synthesis\nno more tool calls allowed]
    Forced --> Save
```

Each iteration is one of five explicit phases (`AgentStepPhase`: `PLAN`, `TOOL_CALL`,
`OBSERVATION`, `REPLAN`, `FINAL`), persisted as an `AgentStep` row and streamed live over the
existing SSE trace mechanism (`SseTraceService`, unchanged) as `planning` / `plan` / `tool_call` /
`observation` / `replan` / `final` / `saving_report` / `complete` / `error` events. The frontend's
`ResearchAgentBody` renders each phase distinctly instead of a flat step list.

### State and memory

Each run is a persisted `AgentRun` row (`agent_runs` table: goal, status, step count, final
report link) with one `AgentStep` row per iteration (`agent_steps` table). This is the agent's
task state and memory:

- **Short-term**: within a run, the growing `List<Message>` conversation (including every past
  tool call and its result) is replayed back to the model each iteration, so it "remembers" what
  it already tried.
- **Long-term**: once a run completes, its `AgentRun`/`AgentStep` rows and the saved
  `ResearchNote` are durable and queryable — unlike the old `SseTraceService`-only trace, which
  was in-memory and lost after ~2 hours or a server restart.

### Tool selection and permission boundary

The research agent's tools (`ResearchAgentTools`: `getCompanyProfile`, `getRecentNews`,
`getMarketIndicator`, `queryKnowledgeBase`) are a **separate Spring bean** from the chat
assistant's (`ChatTools`), and are all read-only. Wiring the orchestrator's `ChatClient` to only
this bean is what stops the research agent from ever touching a trade, an order, or the
watchlist — a permission boundary enforced by Java wiring, not a prompt instruction the model
could be talked out of.

`queryKnowledgeBase` is available to both surfaces as a normal tool call: the model decides for
itself when it needs RAG (historical context, analyst commentary) versus when the raw
profile/news/indicator data is enough — there's no mandatory retrieval step.

### Guardrails and failure recovery

`AgentGuardrailProperties` (`quantedge.ai.agent.*`) bounds the loop:

| Property               | Default | Purpose                                                                 |
| ---------------------- | ------- | ----------------------------------------------------------------------- |
| `max-steps`            | 8       | Hard cap on plan/act/observe iterations before a forced final synthesis |
| `tool-timeout-seconds` | 15      | Per tool-call timeout before it's treated as a failed observation       |
| `tool-max-retries`     | 2       | Retries for a single tool call after a transient failure (e.g. a 429)   |
| `run-timeout-seconds`  | 120     | Wall-clock budget for the whole run, independent of step count          |

Every `ResearchAgentTools` method routes through `RetryingToolExecutor`, which applies the
timeout/retry budget and always returns a result (an `"Error executing <tool>: ..."` string on
exhausted retries) rather than throwing into the conversation. The orchestrator inspects each
tool result: a failure is traced and persisted as a `REPLAN` step and fed back to the model as an
observation, so the next `PLAN` iteration can react to it — try an alternative tool, retry with
different arguments, or proceed with an explicit caveat — the same way the old pipeline's
try/catch degradation worked, but as a model decision instead of hardcoded fallback text. If the
model still hasn't produced a final answer at `max-steps`, the orchestrator forces one final
tool-free call asking it to synthesize a report from whatever was gathered, and marks the run
`MAX_STEPS_REACHED` rather than failing outright.

### Deterministic trade authorization

Trade execution is never gated by the LLM alone. `ChatTools#placeOrder` can only **stage** a
proposal (`PendingOrderService`, in-memory, per-user); there is no tool the model can call to
execute or discard it. The only way a staged trade is executed or discarded is a real
authenticated HTTP request to `POST /api/v1/chat/pending-order/confirm` or `.../cancel`
(`PendingOrderController`), triggered exclusively by the user clicking Accept/Reject on the
`PendingOrderCard` in the UI — never by the model interpreting a "yes"/"no" chat message. This
replaced the previous design, where a system-prompt instruction was the only thing stopping the
model from confirming a trade on its own.

## ✨ Tech Stack

<div align="center">

<table>
  <tr>
    <th width="55%">Layer</th>
    <th width="45%">Technology</th>
  </tr>

  <tr>
    <td><b>Frontend Framework</b></td>
    <td>Next.js (App Router / Turbopack)</td>
  </tr>

  <tr>
    <td><b>Frontend Language</b></td>
    <td>TypeScript</td>
  </tr>

  <tr>
    <td><b>CSS</b></td>
    <td>Tailwind CSS</td>
  </tr>

  <tr>
    <td><b>Charts</b></td>
    <td>Lightweight Charts</td>
  </tr>

  <tr>
    <td><b>State Management</b></td>
    <td>React Query (TanStack)</td>
  </tr>

  <tr>
    <td><b>Animations</b></td>
    <td>Framer Motion</td>
  </tr>

  <tr>
    <td><b>Backend Framework</b></td>
    <td>Spring Boot</td>
  </tr>

  <tr>
    <td><b>Backend Language</b></td>
    <td>Java</td>
  </tr>

  <tr>
    <td><b>API Layer</b></td>
    <td>GraphQL + WebMVC</td>
  </tr>

  <tr>
    <td><b>AI Framework</b></td>
    <td>Spring AI</td>
  </tr>

  <tr>
    <td><b>Security / Auth</b></td>
    <td>Spring Security, OAuth2, JJWT</td>
  </tr>

  <tr>
    <td><b>Database</b></td>
    <td>PostgreSQL (Data JPA + Flyway)</td>
  </tr>

  <tr>
    <td><b>Vector Store</b></td>
    <td>Qdrant</td>
  </tr>

  <tr>
    <td><b>Message Broker</b></td>
    <td>Kafka</td>
  </tr>

  <tr>
    <td><b>LLM Provider</b></td>
    <td>Groq (via Spring AI's OpenAI-compatible client)</td>
  </tr>

  <tr>
    <td><b>Embeddings</b></td>
    <td>Local ONNX (all-MiniLM-L6-v2) — Groq has no embeddings endpoint</td>
  </tr>

  <tr>
    <td><b>File Exports</b></td>
    <td>iText7 (PDF), OpenCSV (CSV)</td>
  </tr>

  <tr>
    <td><b>Testing</b></td>
    <td>JUnit, Testcontainers, Jacoco</td>
  </tr>

</table>

</div>
