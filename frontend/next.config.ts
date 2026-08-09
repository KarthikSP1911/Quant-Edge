import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Keep the dev console to route compile/request lines — no verbose per-fetch
  // cache logging cluttering the terminal.
  logging: {
    fetches: {
      fullUrl: false,
    },
  },
}

export default nextConfig
