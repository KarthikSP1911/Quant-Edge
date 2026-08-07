'use client'

import { useState } from 'react'

// Buy/Sell open the real trade modals in phase-2/fe-trade-modals — these are inert placeholders
// until that slice wires them up. Watchlist toggling here is local-only (no REST endpoint yet).
export default function ActionButtons() {
  const [watchlisted, setWatchlisted] = useState(false)

  return (
    <div className="flex flex-wrap gap-2">
      <button
        type="button"
        disabled
        title="Trade modals land in phase-2/fe-trade-modals"
        className="rounded-md bg-[var(--color-profit)] px-4 py-2 text-sm font-medium text-white opacity-50"
      >
        Buy
      </button>
      <button
        type="button"
        disabled
        title="Trade modals land in phase-2/fe-trade-modals"
        className="rounded-md bg-[var(--color-loss)] px-4 py-2 text-sm font-medium text-white opacity-50"
      >
        Sell
      </button>
      <button
        type="button"
        onClick={() => setWatchlisted((w) => !w)}
        className={`rounded-md border px-4 py-2 text-sm font-medium transition-colors ${
          watchlisted
            ? 'border-[var(--color-accent-blue)] bg-[var(--color-accent-light)] text-[var(--color-accent-blue)]'
            : 'border-[var(--color-border)] text-[var(--color-text-primary)] hover:bg-[var(--color-sidebar-hover)]'
        }`}
      >
        {watchlisted ? '★ Watchlisted' : '☆ Add to Watchlist'}
      </button>
      <button
        type="button"
        disabled
        title="Research agent lands in Phase 6"
        className="rounded-md border border-[var(--color-border)] px-4 py-2 text-sm font-medium text-[var(--color-text-primary)] opacity-50"
      >
        Research this stock
      </button>
    </div>
  )
}
