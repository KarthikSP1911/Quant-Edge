'use client'

import { useMemo } from 'react'
import { useOrderHistory } from '@/hooks/useOrders'
import OrdersTable from '@/components/orders/OrdersTable'
import EmptyState from '@/components/ui/EmptyState'

const PREVIEW_LIMIT = 5

export default function RecentOrdersPreview() {
  const { data, isPending, isError } = useOrderHistory()

  // orderHistory already covers every status (open, filled, cancelled, ...) newest first.
  const orders = useMemo(() => (data ?? []).slice(0, PREVIEW_LIMIT), [data])

  return (
    <section className="flex min-w-0 flex-col rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
      <h2 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">Recent orders</h2>

      {isPending && (
        <div className="h-40 animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
      )}
      {isError && (
        <p className="text-sm text-[var(--color-text-secondary)]">Couldn&apos;t load orders.</p>
      )}
      {!isPending &&
        !isError &&
        (orders.length === 0 ? (
          <EmptyState
            title="No orders yet"
            action={{ label: 'Browse companies to place your first trade', href: '/companies' }}
          />
        ) : (
          <OrdersTable orders={orders} bordered={false} />
        ))}
    </section>
  )
}
