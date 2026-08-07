import type { PortfolioSummary } from '@/types/portfolio'
import { formatChangePercent, formatPrice, formatSignedCurrency } from '@/lib/utils/format'

function SummaryCard({
  label,
  value,
  valueClassName,
  sub,
}: {
  label: string
  value: string
  valueClassName?: string
  sub?: string
}) {
  return (
    <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4">
      <div className="text-xs font-medium text-[var(--color-text-secondary)]">{label}</div>
      <div
        className={valueClassName ?? 'mt-1 text-lg font-semibold text-[var(--color-text-primary)]'}
      >
        {value}
      </div>
      {sub && <div className="mt-0.5 text-xs text-[var(--color-text-secondary)]">{sub}</div>}
    </div>
  )
}

export default function PortfolioSummaryHeader({ summary }: { summary: PortfolioSummary }) {
  const pnlPositive = summary.totalUnrealizedPnl >= 0
  const pnlColor = pnlPositive ? 'text-[var(--color-profit)]' : 'text-[var(--color-loss)]'

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      <SummaryCard label="Market Value" value={formatPrice(summary.totalMarketValue)} />
      <SummaryCard label="Cost Basis" value={formatPrice(summary.totalCostBasis)} />
      <SummaryCard
        label="Unrealized P&L"
        value={formatSignedCurrency(summary.totalUnrealizedPnl)}
        valueClassName={`mt-1 text-lg font-semibold ${pnlColor}`}
        sub={formatChangePercent(summary.totalUnrealizedPnlPercent)}
      />
      <SummaryCard label="Cash Balance" value={formatPrice(summary.cashBalance)} />
    </div>
  )
}
