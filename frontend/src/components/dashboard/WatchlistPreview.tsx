'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useWatchlist, useRemoveFromWatchlist } from '@/hooks/useWatchlist'
import CompanyLogo from '@/components/companies/CompanyLogo'
import EmptyState from '@/components/ui/EmptyState'
import { formatPrice } from '@/lib/utils/format'

const PREVIEW_LIMIT = 6

export default function WatchlistPreview() {
  const { data, isPending, isError } = useWatchlist()
  const removeMutation = useRemoveFromWatchlist()
  const [removingSymbol, setRemovingSymbol] = useState<string | null>(null)
  const companies = (data ?? []).slice(0, PREVIEW_LIMIT)

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
          <EmptyState
            title="Your watchlist is empty"
            action={{ label: 'Browse companies to add some', href: '/companies' }}
          />
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {companies.map((company) => (
              <div
                key={company.id}
                className="flex items-start justify-between gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-3 transition-colors hover:bg-[var(--color-sidebar-hover)]"
              >
                <Link
                  href={`/stocks/${company.symbol}`}
                  className="flex min-w-0 flex-1 items-center gap-2.5"
                >
                  <CompanyLogo symbol={company.symbol} logoUrl={company.logoUrl} />
                  <div className="min-w-0">
                    <div className="font-medium text-[var(--color-text-primary)]">
                      {company.symbol}
                    </div>
                    <div className="truncate text-xs text-[var(--color-text-secondary)]">
                      {company.name}
                    </div>
                  </div>
                </Link>

                <div className="flex shrink-0 flex-col items-end gap-1.5">
                  <button
                    type="button"
                    disabled={removingSymbol === company.symbol}
                    onClick={() => handleRemove(company.symbol)}
                    aria-label={`Remove ${company.symbol} from watchlist`}
                    className="rounded-md p-1 text-[var(--color-text-muted)] transition-colors hover:bg-[var(--color-border)] hover:text-[var(--color-loss)] disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <svg width="12" height="12" viewBox="0 0 18 18" fill="none" aria-hidden="true">
                      <path
                        d="M2 2L16 16M16 2L2 16"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                      />
                    </svg>
                  </button>
                  <span className="text-xs font-medium tabular-nums text-[var(--color-text-primary)]">
                    {formatPrice(company.currentPrice)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        ))}
    </section>
  )
}
