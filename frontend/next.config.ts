import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
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
