'use client'

import Link from 'next/link'
import { usePortfolio } from '@/hooks/usePortfolio'
import HoldingsTable from '@/components/portfolio/HoldingsTable'
import EmptyState from '@/components/ui/EmptyState'

const PREVIEW_LIMIT = 5

export default function HoldingsPreview() {
  const { data, isPending, isError } = usePortfolio()
  const holdings = data?.holdings ?? []

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Holdings</h2>
        <Link
          href="/portfolio"
          className="text-sm font-medium text-[var(--color-accent-blue)] hover:underline"
        >
          View all
        </Link>
      </div>

      {isPending && (
        <div className="h-40 animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
      )}
      {isError && (
        <p className="text-sm text-[var(--color-text-secondary)]">Couldn&apos;t load holdings.</p>
      )}
      {!isPending &&
        !isError &&
        (holdings.length === 0 ? (
          <EmptyState
            title="No holdings yet"
            action={{ label: 'Browse companies to start trading', href: '/companies' }}
          />
        ) : (
          <HoldingsTable holdings={holdings} limit={PREVIEW_LIMIT} />
        ))}
    </section>
  )
}
