# Docker

`docker-compose.yml` (repo root) supports two modes.

## Mode 1 — full local stack

```bash
docker compose --profile local up --build
```

Starts everything: `postgres`, `redis` + `redis-http` (a REST-protocol sidecar — see below),
`zookeeper` + `kafka`, `backend`, `frontend`. No `.env` required; every backend env var has a
Docker-friendly default (see `backend/src/main/resources/application.properties`).

- App (via nginx): http://localhost (set `NGINX_PORT` to use another host port)
- Backend: http://localhost:8080 (health: http://localhost:8080/actuator/health)
- Frontend: http://localhost:3000

## nginx reverse proxy

`nginx` is the single entry point at http://localhost: `/api/*` and `/graphql` go to the backend,
everything else to the frontend, so the browser sees one origin. Routing, timeouts, SSE handling
and TLS notes are in [nginx.md](nginx.md). `FRONTEND_URL` defaults to `http://localhost` to match,
and the frontend image is built with an empty `NEXT_PUBLIC_API_URL`.

**Redis note**: `RedisCacheClient` always speaks Upstash's REST protocol
(`GET /get/{key}`, `POST /set/{key}`), never the native Redis wire protocol — that's how the app
talks to Redis in every environment. Locally there's no Upstash, so `redis-http`
(`hiett/serverless-redis-http`) fronts the plain `redis` container and translates REST calls to
RESP, and `UPSTASH_REDIS_REST_URL` defaults to `http://redis-http:80`.

**Kafka note**: hosted mode (Aiven) needs `SASL_SSL`; local Docker Kafka runs `PLAINTEXT`.
`spring.kafka.security.protocol` and the SASL/SSL-bundle properties are env-driven
(`KAFKA_SECURITY_PROTOCOL`, `KAFKA_SASL_*`, `KAFKA_SSL_BUNDLE`, `KAFKA_SSL_TRUSTSTORE_LOCATION`) so
the same `application.properties` works against both — see `.env.example`.

## Mode 2 — external/hosted infra

Provide a `.env` at the repo root with hosted connection details (Neon Postgres, Upstash Redis,
Aiven Kafka — see `.env.example`), then:

```bash
docker compose up nginx backend frontend --build
```

The `postgres`/`redis`/`redis-http`/`zookeeper`/`kafka` services are gated behind the `local`
profile, so they don't start; `backend` reads connection details straight from `.env`.

## Health checks

`/actuator/health` aggregates: Postgres (Boot's built-in `DataSource` health check), Redis
(custom indicator pinging Upstash's/`redis-http`'s `/ping`), and Kafka (custom indicator running
`AdminClient.describeCluster()` with a 3s timeout, reusing the app's own `KafkaAdmin` config so it
picks up whichever security protocol is active). `/actuator/health/readiness` and
`/actuator/health/liveness` are exposed via Boot's health probe groups for use as Docker/K8s
readiness and liveness checks.

## Deploying to a host

Compose is for local and single-host use. For Render and other managed platforms (separate
services, no Compose) see [deploy.md](deploy.md).
