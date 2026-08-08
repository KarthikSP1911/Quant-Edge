import type { TimeMachineDecision } from '@/types/timeMachine'
import { formatChangePercent, formatPrice } from '@/lib/utils/format'

function DecisionRow({ decision }: { decision: TimeMachineDecision }) {
  const positive = decision.realizedGainPercent >= 0
  return (
    <div className="flex items-center justify-between gap-3 border-b border-[var(--color-border)] px-4 py-3 last:border-b-0">
      <div>
        <div className="font-medium text-[var(--color-text-primary)]">{decision.symbol}</div>
        <div className="text-xs text-[var(--color-text-secondary)]">
          {decision.quantity} sh: {formatPrice(decision.buyPrice)} to {formatPrice(decision.sellPrice)}
        </div>
      </div>
      <span
        className={`font-medium ${positive ? 'text-[var(--color-profit)]' : 'text-[var(--color-loss)]'}`}
      >
        {formatChangePercent(decision.realizedGainPercent)}
      </span>
    </div>
  )
}

export default function TimeMachineDecisionsList({
  title,
  decisions,
}: {
  title: string
  decisions: TimeMachineDecision[]
}) {
  return (
    <div className="rounded-lg border border-[var(--color-border)]">
      <div className="border-b border-[var(--color-border)] bg-[var(--color-card-bg)] px-4 py-2 text-xs font-medium text-[var(--color-text-secondary)]">
        {title}
      </div>
      {decisions.length === 0 ? (
        <p className="px-4 py-6 text-center text-sm text-[var(--color-text-secondary)]">
          No closed positions as of this date.
        </p>
      ) : (
        decisions.map((decision, i) => (
          <DecisionRow key={`${decision.symbol}-${decision.executedAt}-${i}`} decision={decision} />
        ))
      )}
    </div>
  )
}
