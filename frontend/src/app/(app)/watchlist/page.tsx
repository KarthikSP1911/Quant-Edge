'use client'

import { useState } from 'react'
import { useRemoveFromWatchlist, useWatchlist } from '@/hooks/useWatchlist'
import WatchlistTable from '@/components/watchlist/WatchlistTable'
import {
  WatchlistEmpty,
  WatchlistError,
  WatchlistTableSkeleton,
} from '@/components/watchlist/WatchlistStates'

export default function WatchlistPage() {
  const { data: companies, isPending, isError, refetch } = useWatchlist()
  const removeMutation = useRemoveFromWatchlist()
  const [removingSymbol, setRemovingSymbol] = useState<string | null>(null)

  const handleRemove = (symbol: string) => {
    setRemovingSymbol(symbol)
    removeMutation.mutate(symbol, {
      onSettled: () => setRemovingSymbol(null),
    })
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Watchlist</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Companies you&apos;re keeping an eye on.
        </p>
      </div>

      {isPending && <WatchlistTableSkeleton />}
      {isError && <WatchlistError onRetry={() => refetch()} />}
      {!isPending && !isError && companies && companies.length === 0 && <WatchlistEmpty />}
      {!isPending && !isError && companies && companies.length > 0 && (
        <WatchlistTable
          companies={companies}
          onRemove={handleRemove}
          removingSymbol={removingSymbol}
        />
      )}
    </div>
  )
}
