# Deploying QuantEdge

QuantEdge is two stateless containers (Spring Boot backend, Next.js frontend) plus external managed
services. Nothing is host-specific: any platform that runs Docker images and injects `$PORT` works
(Render, Railway, Fly.io, Cloud Run, a VPS). `render.yaml` at the repo root is a ready Render
Blueprint.

## Architecture on a host

```
Browser ──► frontend (Next.js)  ── proxies /api, /graphql ──► backend (Spring Boot)
   └──── SSE and Google sign-in, direct to the backend ─────────►     │
                                          Neon Postgres · Upstash Redis · Aiven Kafka · Groq · Qdrant
```

- **Same-origin by proxy.** On split hosts (`*.onrender.com` subdomains are different sites) the
  refresh-token cookie is `SameSite=Lax`, so a browser would not send it to a different site. The
  frontend therefore proxies `/api/*` and `/graphql` to the backend (`frontend/next.config.ts`,
  driven by `BACKEND_URL`). The browser only sees the frontend origin for API calls and the cookie
  stays first-party.
- **SSE goes direct.** Order-fill and agent-trace streams authenticate with a query-string token (no
  cookie), so they connect straight to the backend via `NEXT_PUBLIC_DIRECT_BACKEND_URL` to avoid
  the proxy buffering them. The backend's CORS allow-list (`FRONTEND_URL` / `CORS_ALLOWED_ORIGINS`)
  must include the frontend origin.
- **Google sign-in goes direct too.** Spring keeps the OAuth2 login state in a server session
  cookie, so the flow must start and finish on the backend host. The sign-in button links to
  `NEXT_PUBLIC_DIRECT_BACKEND_URL`; the backend then redirects to `FRONTEND_URL/oauth2/callback`,
  and the frontend completes login through the proxied `/api/auth/oauth2/callback`.
- **Build-time values.** `NEXT_PUBLIC_*` and `BACKEND_URL` are baked into the frontend at
  `next build`. Changing them needs a redeploy.

## External services (no containers needed)

| Need                        | Service                                           | Env vars                                                                |
| --------------------------- | ------------------------------------------------- | ----------------------------------------------------------------------- |
| Postgres                    | Neon (or Render Postgres)                         | `DATABASE_URL` (JDBC form), `DATABASE_USERNAME`, `DATABASE_PASSWORD`    |
| Redis (REST)                | Upstash                                           | `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN`                    |
| Kafka                       | Aiven (SASL_SSL)                                  | `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_SASL_USERNAME`, `KAFKA_SASL_PASSWORD` |
| LLM / vectors / market data | Groq, Qdrant, Finnhub, Twelve Data, Alpha Vantage | see `.env.example`                                                      |

Render has no managed Kafka or Upstash-style Redis, which is why those stay external. Render's own
Postgres gives a `postgres://` URL; convert it to
`jdbc:postgresql://<host>:5432/<db>?sslmode=require` and pass the user and password separately.
Flyway migrations run on backend startup.

## Deploy on Render

1. Push the repo to GitHub/GitLab. In Render choose **New → Blueprint** and select it; Render reads
   `render.yaml` and creates `quantedge-backend` and `quantedge-frontend`.
2. Fill the `sync: false` prompts. JWT secrets are generated for you. Leave
   `FRONTEND_URL`, `BACKEND_URL` and `NEXT_PUBLIC_DIRECT_BACKEND_URL` for step 3 if you do not know the URLs
   yet (use a placeholder such as `https://placeholder.invalid`).
3. After the first deploy, copy the two public URLs and set:
   - backend `FRONTEND_URL` = `https://<frontend>.onrender.com`
   - frontend `BACKEND_URL` and `NEXT_PUBLIC_DIRECT_BACKEND_URL` = `https://<backend>.onrender.com`

   No trailing slashes. Then redeploy the frontend (the values are build-time) and the backend.

4. **Google sign-in:** in the Google Cloud console add
   `https://<backend>.onrender.com/login/oauth2/code/google` to the OAuth client's authorized
   redirect URIs (the callback lands on the backend host; the backend runs with
   `server.forward-headers-strategy=native` so it builds an `https` URI behind Render's proxy).
5. **Razorpay webhook** (optional): point it at `https://<backend>.onrender.com/api/webhooks/razorpay`
   and set `RAZORPAY_WEBHOOK_SECRET`. Without the secret the endpoint returns 503 by design.

## Other hosts

- **Any Docker host:** build `backend/` and `frontend/` images, set the same env vars, and make
  sure the platform sets `PORT` (or accept the defaults, 8080 and 3000). Health check:
  `/actuator/health/liveness` (backend) and `/login` (frontend).
- **Custom domain with a shared parent** (`app.example.com` and `api.example.com`): the cookie is
  same-site, so you can skip the proxy. Leave `BACKEND_URL` empty, set `NEXT_PUBLIC_API_URL` to the
  API origin, and add the frontend origin to `CORS_ALLOWED_ORIGINS`.
- **Fully cross-site, no proxy:** set `COOKIE_SAMESITE=None` and `COOKIE_SECURE=true`. Browsers that
  block third-party cookies will still break login, so prefer the proxy.
- **nginx single origin** (`docker-compose.yml`): see `docs/docker.md`.

## Things to know

- **Memory.** The JVM sizes its heap to 70% of the container limit (`JAVA_OPTS`). 512MB works for
  light use; 1GB is comfortable.
- **Cold starts.** Free tiers sleep when idle. The backend's Kafka consumer and SSE streams need an
  always-on instance, so use a paid plan for the backend.
- **Exports.** PDF/CSV exports are written to `./exports` inside the container (`EXPORT_BASE_PATH`)
  and are lost on redeploy. Mount a persistent disk there if you need them to survive.
- **Secrets.** Never commit `.env`. Set secrets in the host's dashboard.
