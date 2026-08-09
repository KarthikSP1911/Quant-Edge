'use client'

import Link from 'next/link'
import { useMemo } from 'react'
import { useOpenOrders, useOrderHistory } from '@/hooks/useOrders'
import OrdersTable from '@/components/orders/OrdersTable'
import EmptyState from '@/components/ui/EmptyState'

const PREVIEW_LIMIT = 5

export default function RecentOrdersPreview() {
  const openOrders = useOpenOrders()
  const orderHistory = useOrderHistory()

  const isPending = openOrders.isPending || orderHistory.isPending
  const isError = openOrders.isError || orderHistory.isError

  const orders = useMemo(() => {
    const combined = [...(openOrders.data ?? []), ...(orderHistory.data ?? [])]
    return combined
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, PREVIEW_LIMIT)
  }, [openOrders.data, orderHistory.data])

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Recent orders</h2>
        <Link
          href="/orders"
          className="text-sm font-medium text-[var(--color-accent-blue)] hover:underline"
        >
          View all
        </Link>
      </div>

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
          <OrdersTable orders={orders} />
        ))}
    </section>
  )
}
