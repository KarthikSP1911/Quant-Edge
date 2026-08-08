'use client'

import { usePortfolio } from '@/hooks/usePortfolio'
import PortfolioSummaryHeader from '@/components/portfolio/PortfolioSummaryHeader'
import HoldingsTable from '@/components/portfolio/HoldingsTable'
import ExportButtons from '@/components/shared/ExportButtons'
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
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Portfolio</h1>
          <p className="text-sm text-[var(--color-text-secondary)]">
            Track your simulated holdings and performance.
          </p>
        </div>
        <ExportButtons />
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
