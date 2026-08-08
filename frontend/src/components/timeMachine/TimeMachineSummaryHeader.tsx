import type { TimeMachineResult } from '@/types/timeMachine'
import { formatPrice } from '@/lib/utils/format'

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4">
      <div className="text-xs font-medium text-[var(--color-text-secondary)]">{label}</div>
      <div className="mt-1 text-lg font-semibold text-[var(--color-text-primary)]">{value}</div>
    </div>
  )
}

export default function TimeMachineSummaryHeader({ result }: { result: TimeMachineResult }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      <SummaryCard label="Cash Balance" value={formatPrice(result.cashBalance)} />
      <SummaryCard label="Holdings Value" value={formatPrice(result.totalMarketValue)} />
      <SummaryCard label="Total Account Value" value={formatPrice(result.totalAccountValue)} />
    </div>
  )
}
