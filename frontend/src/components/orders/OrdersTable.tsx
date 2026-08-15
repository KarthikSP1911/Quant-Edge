'use client'

import CompanyLogo from '@/components/companies/CompanyLogo'
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
  bordered = true,
}: {
  orders: Order[]
  onCancel?: (orderId: string) => void
  cancellingId?: string | null
  /** Set false when already nested inside a bordered card, to avoid a double border. */
  bordered?: boolean
}) {
  return (
    <div
      className={`overflow-x-auto ${bordered ? 'rounded-lg border border-[var(--color-border)]' : ''}`}
    >
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead>
          <tr className="border-b border-[var(--color-border)] bg-[var(--color-card-bg)] text-xs text-[var(--color-text-secondary)]">
            <th className="px-4 py-3 font-medium whitespace-nowrap">Symbol</th>
            <th className="px-4 py-3 font-medium whitespace-nowrap">Side</th>
            <th className="px-4 py-3 font-medium whitespace-nowrap">Type</th>
            <th className="px-4 py-3 font-medium whitespace-nowrap">Quantity</th>
            <th className="px-4 py-3 font-medium whitespace-nowrap">Limit / Stop</th>
            <th className="px-4 py-3 font-medium whitespace-nowrap">Status</th>
            <th className="px-4 py-3 font-medium whitespace-nowrap">Placed</th>
            {onCancel && <th className="px-4 py-3 text-right font-medium"></th>}
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id} className="border-b border-[var(--color-border)] last:border-b-0">
              <td className="px-4 py-3 font-medium whitespace-nowrap text-[var(--color-text-primary)]">
                <div className="flex items-center gap-3">
                  <CompanyLogo symbol={order.symbol} logoUrl={order.company.logoUrl} />
                  {order.symbol}
                </div>
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-[var(--color-text-secondary)]">
                {order.side}
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-[var(--color-text-secondary)]">
                {order.type}
              </td>
              <td className="px-4 py-3 whitespace-nowrap tabular-nums text-[var(--color-text-secondary)]">
                {order.filledQuantity} / {order.quantity}
              </td>
              <td className="px-4 py-3 whitespace-nowrap tabular-nums text-[var(--color-text-secondary)]">
                {formatPrice(order.limitPrice)} / {formatPrice(order.stopPrice)}
              </td>
              <td className="px-4 py-3 whitespace-nowrap">
                <OrderStatusBadge status={order.status} />
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-[var(--color-text-secondary)]">
                {formatTimestamp(order.createdAt)}
              </td>
              {onCancel && (
                <td className="px-4 py-3 text-right whitespace-nowrap">
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
