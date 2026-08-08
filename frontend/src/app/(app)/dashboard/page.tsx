'use client'

import { useDashboard } from '@/hooks/useDashboard'
import { DashboardSkeleton, DashboardError } from '@/components/dashboard/DashboardStates'
import AllocationChart from '@/components/dashboard/AllocationChart'
import RecentActivity from '@/components/dashboard/RecentActivity'
import ExportButtons from '@/components/shared/ExportButtons'
import { formatPrice, formatChangePercent } from '@/lib/utils/format'

export default function DashboardPage() {
  const { data, isLoading, isError, refetch } = useDashboard()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-[var(--color-text-primary)]">Dashboard</h1>
          <p className="text-sm text-[var(--color-text-secondary)]">
            Your simulated account balance, allocation, and recent activity.
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

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <section className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
              <h2 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
                Allocation by sector
              </h2>
              <AllocationChart allocation={data.allocation} />
            </section>

            <section className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
              <h2 className="mb-2 text-sm font-semibold text-[var(--color-text-primary)]">
                Recent activity
              </h2>
              <RecentActivity activity={data.recentActivity} />
            </section>
          </div>
        </>
      )}
    </div>
  )
}

function BalanceSummary({
  cashBalance,
  netWorth,
  dayChangeValue,
  dayChangePercent,
}: {
  cashBalance: number
  netWorth: number
  dayChangeValue: number
  dayChangePercent: number
}) {
  const isPositive = dayChangeValue >= 0
  const changeColor = isPositive ? 'var(--color-profit)' : 'var(--color-loss)'
  const sign = isPositive ? '+' : ''

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <SummaryTile label="Cash balance" value={formatPrice(cashBalance)} />
      <SummaryTile label="Total net worth" value={formatPrice(netWorth)} />
      <div className="flex flex-col gap-1 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
        <span className="text-xs font-medium text-[var(--color-text-secondary)]">Day change</span>
        <span className="text-xl font-semibold tabular-nums" style={{ color: changeColor }}>
          {sign}
          {formatPrice(dayChangeValue)} ({formatChangePercent(dayChangePercent)})
        </span>
      </div>
    </div>
  )
}

function SummaryTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
      <span className="text-xs font-medium text-[var(--color-text-secondary)]">{label}</span>
      <span className="text-xl font-semibold tabular-nums text-[var(--color-text-primary)]">
        {value}
      </span>
    </div>
  )
}
