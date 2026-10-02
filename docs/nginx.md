# nginx reverse proxy

`nginx/nginx.conf` puts the whole app behind one origin. It is the entry point of the Docker
Compose stack and the recommended setup for any single-host deployment (a VPS, a home server).
Platforms that already provide their own routing (Render, Railway) do not need it; see
[deploy.md](deploy.md).

```
Browser ──► nginx :80 ──┬─ /api/*, /graphql ─► backend  :8080 (Spring Boot)
                        └─ everything else ──► frontend :3000 (Next.js)
```

## Why a single origin

- The refresh-token cookie is `SameSite=Lax`. It is only sent to the site that set it, so the API
  and the UI must share a site. One origin guarantees that.
- No CORS preflights for normal calls, because the browser never makes a cross-origin request.
- The frontend is built with an empty `NEXT_PUBLIC_API_URL`, so it calls relative URLs (`/api/...`,
  `/graphql`) that work on any hostname without a rebuild.

## Run it

```bash
docker compose --profile local up --build          # full local stack including nginx
docker compose up nginx backend frontend --build   # with hosted Neon/Upstash/Aiven (.env at root)
```

The app is at http://localhost. To use another host port, set `NGINX_PORT` (for example
`NGINX_PORT=8000`); then `FRONTEND_URL` must match, e.g. `http://localhost:8000`.

`nginx -t` validates the config without starting the stack:

```bash
docker run --rm -v "$PWD/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
  --add-host backend:127.0.0.1 --add-host frontend:127.0.0.1 nginx:1.27-alpine nginx -t
```

## Routing table

| Location                                          | Upstream     | Settings                                    | Why                                                                                                                              |
| ------------------------------------------------- | ------------ | ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `/healthz`                                        | nginx itself | `return 200`                                | Compose healthcheck; independent of the upstreams                                                                                |
| `/api/orders/stream`, `/api/v1/agent/trace/*`     | backend      | buffering off, gzip off, 1860s read timeout | SSE. Buffering would hold events until a buffer fills. The backend's emitters last 30 minutes, so the timeout is just above that |
| `/api/v1/chat/*`, `/api/v1/agent/*`               | backend      | 180s read timeout                           | Chat and the research agent wait on Groq, well past nginx's 60s default                                                          |
| `/api/webhooks/razorpay`                          | backend      | request buffering on                        | The raw body must arrive untouched so the HMAC signature still verifies                                                          |
| `/api/*`, `/graphql`                              | backend      | 60s read timeout                            | Normal REST and GraphQL                                                                                                          |
| `/oauth2/authorization/*`, `/login/oauth2/code/*` | backend      | defaults                                    | Spring Security's Google sign-in endpoints. Not all of `/oauth2/*`: the frontend owns the `/oauth2/callback` page                |
| `/`                                               | frontend     | websocket upgrade headers                   | Pages, static assets, Next.js dev HMR                                                                                            |

Regex locations take priority over the `/api/` prefix, so SSE and chat never fall into the generic
block.

## Forwarded headers

nginx sends `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` and `X-Forwarded-Host`.
The backend sets `server.forward-headers-strategy=native`, so:

- audit logs record the client's IP, not nginx's;
- OAuth2 builds its redirect URI from the public host and scheme, not `backend:8080`.

## Configuration that must agree

| Setting                                    | Value behind nginx                                                                                |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------- |
| `FRONTEND_URL` (backend)                   | the public origin, e.g. `http://localhost`. It is the CORS allow-list and the post-login redirect |
| `NEXT_PUBLIC_API_URL` (frontend build arg) | empty                                                                                             |
| `COOKIE_SECURE`                            | `false` over plain HTTP, `true` once TLS is in front                                              |
| Google OAuth redirect URI                  | `http://localhost/login/oauth2/code/google` (add `:<NGINX_PORT>` if not 80)                       |

## Adding TLS

The shipped config listens on port 80 only. For a real host, either terminate TLS in front of nginx
(a cloud load balancer, Caddy, Cloudflare) or add a `listen 443 ssl` server block with your
certificate, then set `COOKIE_SECURE=true` and use the `https://` origin for `FRONTEND_URL` and the
Google redirect URI. `X-Forwarded-Proto` is already passed through.

## Troubleshooting

| Symptom                                                                   | Likely cause                                                                                                                |
| ------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| Live order notifications or the agent trace stall, then arrive in a burst | A proxy in front of nginx is buffering SSE, or the request is not hitting the SSE locations                                 |
| Login works, but a reload logs you out                                    | `FRONTEND_URL` does not match the browser's origin, or the page is on a different host than the API                         |
| `502 Bad Gateway` right after `up`                                        | A container is still starting; nginx resolves `backend` and `frontend` at boot. Check `docker compose ps` and restart nginx |
| Razorpay webhook returns 400                                              | A proxy in front of nginx is rewriting the body; the signature is computed over the raw bytes                               |
| Google sign-in: `redirect_uri_mismatch`                                   | The redirect URI in the Google console does not match the public origin                                                     |
