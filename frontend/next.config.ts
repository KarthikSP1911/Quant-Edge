import type { NextConfig } from 'next'

// Server-side URL of the Spring Boot backend, e.g. https://quantedge-backend.onrender.com. When
// set, Next.js proxies /api and /graphql to it, so the browser only ever
// talks to this origin and the refresh-token cookie stays first-party (no third-party-cookie
// blocking on split *.onrender.com domains). Rewrites are baked in at build time, so this must be
// available as a build arg/env var when `next build` runs. Leave unset for local dev and for the
// nginx setup, where the frontend is already served same-origin with the API.
const backendUrl = process.env.BACKEND_URL?.replace(/\/+$/, '')

const nextConfig: NextConfig = {
  // Self-contained server bundle (.next/standalone/server.js) for a slim Docker runtime image.
  output: 'standalone',
  // Chat and the research agent call Groq and can run well past the 30s default proxy timeout.
  experimental: {
    proxyTimeout: 180_000,
  },
  async rewrites() {
    if (!backendUrl) {
      return []
    }
    return [
      { source: '/api/:path*', destination: `${backendUrl}/api/:path*` },
      { source: '/graphql', destination: `${backendUrl}/graphql` },
    ]
  },
  // /frontend and the repo root each have their own package-lock.json (per CLAUDE.md's
  // separate `npm install` steps, not an npm workspace) - without this, Turbopack can't
  // tell which one is the project root and prints a "workspace root" warning on every
  // `next dev` start.
  turbopack: {
    root: import.meta.dirname,
  },
  // Keep the dev console to route compile/request lines — no verbose per-fetch
  // cache logging cluttering the terminal.
  logging: {
    fetches: {
      fullUrl: false,
    },
    // This app is almost entirely client components talking to the Spring Boot
    // backend (REST/GraphQL/SSE), so client-side console.warn/error (e.g. the SSE
    // parse failure in ResearchAgentBody) would otherwise only show in browser
    // devtools. Forward warn+error to the terminal; skip plain console.log to
    // avoid drowning the compile/request lines in noise.
    browserToTerminal: 'warn',
  },
}

export default nextConfig
