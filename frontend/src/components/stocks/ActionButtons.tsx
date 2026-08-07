'use client'

import { useState } from 'react'
import TradeModal from '@/components/trade/TradeModal'
import { useAddToWatchlist, useRemoveFromWatchlist, useWatchlist } from '@/hooks/useWatchlist'
import type { OrderSide } from '@/types/order'

export default function ActionButtons({ symbol, price }: { symbol: string; price: number }) {
  const [tradeSide, setTradeSide] = useState<OrderSide | null>(null)

  const { data: watchlist } = useWatchlist()
  const addMutation = useAddToWatchlist()
  const removeMutation = useRemoveFromWatchlist()

  const isWatched = watchlist?.some((company) => company.symbol === symbol) ?? false
  const isToggling = addMutation.isPending || removeMutation.isPending

  const toggleWatchlist = () => {
    if (isWatched) {
      removeMutation.mutate(symbol)
    } else {
      addMutation.mutate(symbol)
    }
  }

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
        disabled={isToggling}
        onClick={toggleWatchlist}
        className={`rounded-md border px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
          isWatched
            ? 'border-[var(--color-accent-blue)] bg-[var(--color-accent-light)] text-[var(--color-accent-blue)]'
            : 'border-[var(--color-border)] text-[var(--color-text-primary)] hover:bg-[var(--color-sidebar-hover)]'
        }`}
      >
        {isWatched ? '★ Watchlisted' : '☆ Add to Watchlist'}
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
