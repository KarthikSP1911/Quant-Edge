'use client'

import { formatPrice } from '@/lib/utils/format'
import type { Order } from '@/types/order'
import OrderStatusBadge from './OrderStatusBadge'

function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export default function OrdersTable({
  orders,
  onCancel,
  cancellingId,
}: {
  orders: Order[]
  onCancel?: (orderId: string) => void
  cancellingId?: string | null
}) {
  return (
    <div className="overflow-x-auto rounded-lg border border-[var(--color-border)]">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-[var(--color-border)] bg-[var(--color-card-bg)] text-xs text-[var(--color-text-secondary)]">
            <th className="px-4 py-3 font-medium">Symbol</th>
            <th className="px-4 py-3 font-medium">Side</th>
            <th className="px-4 py-3 font-medium">Type</th>
            <th className="px-4 py-3 font-medium">Quantity</th>
            <th className="px-4 py-3 font-medium">Limit / Stop</th>
            <th className="px-4 py-3 font-medium">Status</th>
            <th className="px-4 py-3 font-medium">Placed</th>
            {onCancel && <th className="px-4 py-3 text-right font-medium"></th>}
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id} className="border-b border-[var(--color-border)] last:border-b-0">
              <td className="px-4 py-3 font-medium text-[var(--color-text-primary)]">
                {order.symbol}
              </td>
              <td className="px-4 py-3 text-[var(--color-text-secondary)]">{order.side}</td>
              <td className="px-4 py-3 text-[var(--color-text-secondary)]">{order.type}</td>
              <td className="px-4 py-3 text-[var(--color-text-secondary)]">
                {order.filledQuantity} / {order.quantity}
              </td>
              <td className="px-4 py-3 text-[var(--color-text-secondary)]">
                {formatPrice(order.limitPrice)} / {formatPrice(order.stopPrice)}
              </td>
              <td className="px-4 py-3">
                <OrderStatusBadge status={order.status} />
              </td>
              <td className="px-4 py-3 text-[var(--color-text-secondary)]">
                {formatTimestamp(order.createdAt)}
              </td>
              {onCancel && (
                <td className="px-4 py-3 text-right">
                  {order.status === 'OPEN' && (
                    <button
                      type="button"
                      disabled={cancellingId === order.id}
                      onClick={() => onCancel(order.id)}
                      className="rounded-md border border-[var(--color-border)] px-3 py-1.5 text-xs font-medium text-[var(--color-text-secondary)] transition-colors hover:border-[var(--color-loss)] hover:text-[var(--color-loss)] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {cancellingId === order.id ? 'Cancelling…' : 'Cancel'}
                    </button>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
