import { formatPrice, formatChangePercent } from '@/lib/utils/format'

export function SummaryTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
      <span className="text-xs font-medium text-[var(--color-text-secondary)]">{label}</span>
      <span className="text-xl font-semibold tabular-nums text-[var(--color-text-primary)]">
        {value}
      </span>
    </div>
  )
}

export default function BalanceSummary({
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
