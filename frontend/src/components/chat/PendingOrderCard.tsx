'use client'

import { useDashboard } from '@/hooks/useDashboard'
import { formatPrice } from '@/lib/utils/format'
import type { PendingOrder } from '@/types/order'

const TYPE_LABELS: Record<PendingOrder['type'], string> = {
  MARKET: 'Market',
  LIMIT: 'Limit',
  STOP_LOSS: 'Stop-loss',
  STOP_LIMIT: 'Stop-limit',
}

export default function PendingOrderCard({
  order,
  onAccept,
  onReject,
  isSubmitting,
}: {
  order: PendingOrder
  onAccept: () => void
  onReject: () => void
  isSubmitting: boolean
}) {
  const { data: dashboard } = useDashboard()
  const isBuy = order.side === 'BUY'
  const hasLimitPrice = order.type === 'LIMIT' || order.type === 'STOP_LIMIT'
  const hasStopPrice = order.type === 'STOP_LOSS' || order.type === 'STOP_LIMIT'

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
              isBuy
                ? 'bg-[var(--color-profit)]/10 text-[var(--color-profit)]'
                : 'bg-[var(--color-loss)]/10 text-[var(--color-loss)]'
            }`}
          >
            {order.side}
          </span>
          <span className="text-sm font-semibold text-[var(--color-text-primary)]">
            {order.quantity} {order.quantity === 1 ? 'share' : 'shares'} of {order.symbol}
          </span>
        </div>
        <span className="text-xs font-medium text-[var(--color-text-secondary)]">
          {TYPE_LABELS[order.type]} order
        </span>
      </div>

      {(hasLimitPrice || hasStopPrice) && (
        <div className="flex gap-2">
          {hasLimitPrice && (
            <div className="flex-1 rounded-md border border-[var(--color-border)] bg-[var(--color-sidebar-hover)] px-3 py-2">
              <p className="text-xs text-[var(--color-text-muted)]">Limit price</p>
              <p className="text-sm font-semibold tabular-nums text-[var(--color-text-primary)]">
                {formatPrice(order.limitPrice)}
              </p>
            </div>
          )}
          {hasStopPrice && (
            <div className="flex-1 rounded-md border border-[var(--color-border)] bg-[var(--color-sidebar-hover)] px-3 py-2">
              <p className="text-xs text-[var(--color-text-muted)]">Stop price</p>
              <p className="text-sm font-semibold tabular-nums text-[var(--color-text-primary)]">
                {formatPrice(order.stopPrice)}
              </p>
            </div>
          )}
        </div>
      )}

      <div className="flex items-center justify-between border-t border-[var(--color-border)] pt-3 text-sm">
        <span className="text-[var(--color-text-secondary)]">
          Estimated {isBuy ? 'cost' : 'proceeds'}
        </span>
        <span className="font-semibold tabular-nums text-[var(--color-text-primary)]">
          {formatPrice(order.estimatedCost)}
        </span>
      </div>

      <div className="flex items-center justify-between text-sm">
        <span className="text-[var(--color-text-secondary)]">Cash balance</span>
        <span className="font-semibold tabular-nums text-[var(--color-text-primary)]">
          {dashboard ? formatPrice(dashboard.cashBalance) : '—'}
        </span>
      </div>

      <div className="flex gap-2 pt-1">
        <button
          type="button"
          onClick={onReject}
          disabled={isSubmitting}
          className="flex-1 rounded-md border border-[var(--color-border)] py-2 text-sm font-medium text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-sidebar-hover)] disabled:cursor-not-allowed disabled:opacity-50"
        >
          Reject
        </button>
        <button
          type="button"
          onClick={onAccept}
          disabled={isSubmitting}
          className="flex-1 rounded-md bg-[var(--color-accent-blue)] py-2 text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSubmitting ? 'Confirming…' : 'Accept'}
        </button>
      </div>
    </div>
  )
}
