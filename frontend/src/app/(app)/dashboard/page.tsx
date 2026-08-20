'use client'

import { useDashboard } from '@/hooks/useDashboard'
import { DashboardSkeleton, DashboardError } from '@/components/dashboard/DashboardStates'
import AllocationChart from '@/components/dashboard/AllocationChart'
import BalanceSummary from '@/components/dashboard/BalanceSummary'
import HoldingsPreview from '@/components/dashboard/HoldingsPreview'
import WatchlistPreview from '@/components/dashboard/WatchlistPreview'
import QuickActions from '@/components/dashboard/QuickActions'
import ExportButtons from '@/components/shared/ExportButtons'

export default function DashboardPage() {
  const { data, isLoading, isError, refetch } = useDashboard()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Dashboard</h1>
          <p className="text-sm text-[var(--color-text-secondary)]">
            Your simulated account balance, holdings, and sector allocation.
          </p>
        </div>
        <ExportButtons />
      </div>

      {isLoading && <DashboardSkeleton />}
      {isError && !isLoading && <DashboardError onRetry={() => void refetch()} />}

      {data && !isLoading && !isError && (
        <>
          <BalanceSummary
            cashBalance={data.cashBalance}
            netWorth={data.netWorth}
            dayChangeValue={data.dayChangeValue}
            dayChangePercent={data.dayChangePercent}
          />

          <QuickActions />

          <HoldingsPreview />

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <section>
              <div className="mb-4 flex w-48 justify-center pl-5">
                <h2 className="text-lg font-semibold whitespace-nowrap text-[var(--color-text-primary)]">
                  Allocation by sector
                </h2>
              </div>
              <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
                <AllocationChart allocation={data.allocation} />
              </div>
            </section>

            <WatchlistPreview />
          </div>
        </>
      )}
    </div>
  )
}
