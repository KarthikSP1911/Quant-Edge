'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useWatchlist, useRemoveFromWatchlist } from '@/hooks/useWatchlist'
import WatchlistTable from '@/components/watchlist/WatchlistTable'

const PREVIEW_LIMIT = 5

export default function WatchlistPreview() {
  const { data, isPending, isError } = useWatchlist()
  const removeMutation = useRemoveFromWatchlist()
  const [removingSymbol, setRemovingSymbol] = useState<string | null>(null)
  const companies = data ?? []

  const handleRemove = (symbol: string) => {
    setRemovingSymbol(symbol)
    removeMutation.mutate(symbol, {
      onSettled: () => setRemovingSymbol(null),
    })
  }

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Watchlist</h2>
        <Link
          href="/watchlist"
          className="text-sm font-medium text-[var(--color-accent-blue)] hover:underline"
        >
          View all
        </Link>
      </div>

      {isPending && (
        <div className="h-40 animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
      )}
      {isError && (
        <p className="text-sm text-[var(--color-text-secondary)]">Couldn&apos;t load watchlist.</p>
      )}
      {!isPending &&
        !isError &&
        (companies.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-10 text-center">
            <p className="text-sm font-medium text-[var(--color-text-primary)]">
              Your watchlist is empty
            </p>
            <Link
              href="/companies"
              className="text-sm font-medium text-[var(--color-accent-blue)] hover:underline"
            >
              Browse companies to add some
            </Link>
          </div>
        ) : (
          <WatchlistTable
            companies={companies}
            onRemove={handleRemove}
            removingSymbol={removingSymbol}
            limit={PREVIEW_LIMIT}
          />
        ))}
    </section>
  )
}
