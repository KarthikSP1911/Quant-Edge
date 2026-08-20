'use client'

import { useEffect, useState } from 'react'
import { getMarketStatus } from '@/lib/marketHours'

const DISMISS_KEY = 'quantedge-market-banner-dismissed'

export default function MarketStatusBanner() {
  const [message, setMessage] = useState<string | null>(null)

  // Reads sessionStorage/wall-clock market hours, both browser-only — this must stay in an
  // effect (not computed during render) to render null on the server and avoid a hydration
  // mismatch; the setState-in-effect lint rule doesn't apply to this one-time, mount-only read.
  useEffect(() => {
    const status = getMarketStatus()
    if (status.isOpen) return

    let dismissed = false
    try {
      dismissed = sessionStorage.getItem(DISMISS_KEY) === 'true'
    } catch {
      // sessionStorage unavailable (e.g. blocked by privacy settings) — show the banner anyway
    }
    if (dismissed) return

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMessage(status.message)
  }, [])

  const dismiss = () => {
    setMessage(null)
    try {
      sessionStorage.setItem(DISMISS_KEY, 'true')
    } catch {
      // best-effort — banner still stays dismissed for this component instance
    }
  }

  if (!message) return null

  return (
    <div className="flex items-center gap-3 bg-[var(--color-accent-blue)] px-4 py-2.5 text-sm font-medium text-white sm:px-6">
      <div className="flex flex-1 items-center justify-center gap-2 pl-6">
        <svg
          width="15"
          height="15"
          viewBox="0 0 18 18"
          fill="none"
          aria-hidden="true"
          className="shrink-0 opacity-90"
        >
          <circle cx="9" cy="9" r="7.25" stroke="currentColor" strokeWidth="1.5" />
          <path
            d="M9 5V9L11.5 10.5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        <p className="min-w-0">{message}</p>
      </div>
      <button
        type="button"
        onClick={dismiss}
        aria-label="Dismiss market hours banner"
        className="relative z-10 shrink-0 cursor-pointer rounded-md p-1 opacity-90 transition-colors hover:bg-white/15 hover:opacity-100"
      >
        <svg width="14" height="14" viewBox="0 0 18 18" fill="none" aria-hidden="true">
          <path
            d="M2 2L16 16M16 2L2 16"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
        </svg>
      </button>
    </div>
  )
}
