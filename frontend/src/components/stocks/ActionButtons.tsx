'use client'

import { useState } from 'react'
import TradeModal from '@/components/trade/TradeModal'
import type { OrderSide } from '@/types/order'

// Watchlist toggling here is local-only (no REST endpoint yet).
export default function ActionButtons({ symbol, price }: { symbol: string; price: number }) {
  const [watchlisted, setWatchlisted] = useState(false)
  const [tradeSide, setTradeSide] = useState<OrderSide | null>(null)

  return (
    <div className="flex flex-wrap gap-2">
      <button
        type="button"
        onClick={() => setTradeSide('BUY')}
        className="rounded-md bg-[var(--color-profit)] px-4 py-2 text-sm font-medium text-white hover:opacity-90"
      >
        Buy
      </button>
      <button
        type="button"
        onClick={() => setTradeSide('SELL')}
        className="rounded-md bg-[var(--color-loss)] px-4 py-2 text-sm font-medium text-white hover:opacity-90"
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

      {tradeSide && (
        <TradeModal
          symbol={symbol}
          side={tradeSide}
          price={price}
          onClose={() => setTradeSide(null)}
        />
      )}
    </div>
  )
}
