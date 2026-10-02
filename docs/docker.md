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

`nginx` (`nginx/nginx.conf`) is the single entry point. `/api/*`, `/graphql` and the Google
OAuth2 paths go to the backend; everything else goes to the Next.js frontend. Because the browser
sees one origin, the auth cookies stay first-party and there are no CORS preflights.

- The frontend image is built with an empty `NEXT_PUBLIC_API_URL`, so it calls relative URLs.
  That value is inlined into the browser bundle at build time; rebuild with `--build` after
  changing it.
- SSE routes (`/api/orders/stream`, `/api/v1/agent/trace/*`) have buffering off and a 31-minute
  read timeout to cover the backend's 30-minute emitters. Chat and agent routes allow 180s for
  Groq. Other API routes use 60s.
- `server.forward-headers-strategy=native` makes Spring trust `X-Forwarded-*`, so audit logs record
  the client IP and OAuth2 redirect URIs use the public host.
- Google sign-in: add `http://localhost/login/oauth2/code/google` to the OAuth client's
  authorized redirect URIs (port 80 only; include `:<NGINX_PORT>` otherwise).
- `FRONTEND_URL` defaults to `http://localhost`. It must equal the public origin, since it is the
  backend's CORS allow-list and the post-login redirect target.
- nginx serves plain HTTP only. For a real deployment, terminate TLS in front of it (or extend the
  config) and set `COOKIE_SECURE=true`.

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
docker compose up backend frontend --build
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
