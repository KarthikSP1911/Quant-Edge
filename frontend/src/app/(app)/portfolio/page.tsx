'use client'

import Link from 'next/link'
import { usePortfolio } from '@/hooks/usePortfolio'
import PortfolioSummaryHeader from '@/components/portfolio/PortfolioSummaryHeader'
import HoldingsTable from '@/components/portfolio/HoldingsTable'
import {
  PortfolioEmpty,
  PortfolioError,
  PortfolioSummarySkeleton,
  PortfolioTableSkeleton,
} from '@/components/portfolio/PortfolioListStates'

export default function PortfolioPage() {
  const { data: portfolio, isPending, isError, refetch } = usePortfolio()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Portfolio</h1>
          <p className="text-sm text-[var(--color-text-secondary)]">
            Track your simulated holdings and performance.
          </p>
        </div>
        <Link
          href="/portfolio/time-machine"
          className="shrink-0 rounded-md border border-[var(--color-border)] px-3 py-1.5 text-sm font-medium text-[var(--color-text-primary)] transition-colors hover:bg-[var(--color-sidebar-hover)]"
        >
          Time Machine
        </Link>
      </div>

      {isPending && (
        <>
          <PortfolioSummarySkeleton />
          <PortfolioTableSkeleton />
        </>
      )}

      {isError && <PortfolioError onRetry={() => refetch()} />}

      {!isPending && !isError && portfolio && (
        <>
          <PortfolioSummaryHeader summary={portfolio.summary} />
          {portfolio.holdings.length === 0 ? (
            <PortfolioEmpty />
          ) : (
            <HoldingsTable holdings={portfolio.holdings} />
          )}
        </>
      )}
    </div>
  )
}
