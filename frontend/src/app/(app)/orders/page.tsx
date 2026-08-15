'use client'

import { useState } from 'react'
import { useCancelOrder, useFilledOrders, useOpenOrders, useOrderHistory } from '@/hooks/useOrders'
import { useDashboard } from '@/hooks/useDashboard'
import OrdersTable from '@/components/orders/OrdersTable'
import { OrdersEmpty, OrdersError, OrdersTableSkeleton } from '@/components/orders/OrdersStates'
import RecentOrdersPreview from '@/components/orders/RecentOrdersPreview'
import RecentActivity from '@/components/orders/RecentActivity'

const TABS = [
  { key: 'open', label: 'Open' },
  { key: 'filled', label: 'Filled' },
  { key: 'history', label: 'History' },
] as const

type TabKey = (typeof TABS)[number]['key']

export default function OrdersPage() {
  const [tab, setTab] = useState<TabKey>('open')

  const openOrders = useOpenOrders()
  const filledOrders = useFilledOrders()
  const orderHistory = useOrderHistory()
  const cancelMutation = useCancelOrder()
  const dashboard = useDashboard()
  const [cancellingId, setCancellingId] = useState<string | null>(null)

  const handleCancel = (orderId: string) => {
    setCancellingId(orderId)
    cancelMutation.mutate(orderId, {
      onSettled: () => setCancellingId(null),
    })
  }

  const active = tab === 'open' ? openOrders : tab === 'filled' ? filledOrders : orderHistory
  const emptyMessage =
    tab === 'open'
      ? 'No open orders'
      : tab === 'filled'
        ? 'No filled orders yet'
        : 'No order history yet'

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Orders</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Track and manage your open, filled, and historical orders.
        </p>
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex gap-1 border-b border-[var(--color-border)]">
          {TABS.map((t) => (
            <button
              key={t.key}
              type="button"
              onClick={() => setTab(t.key)}
              className={`border-b-2 px-4 py-2 text-sm font-medium whitespace-nowrap transition-colors ${
                tab === t.key
                  ? 'border-[var(--color-accent-blue)] text-[var(--color-accent-blue)]'
                  : 'border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {active.isPending && <OrdersTableSkeleton />}
        {active.isError && <OrdersError onRetry={() => active.refetch()} />}
        {!active.isPending &&
          !active.isError &&
          active.data &&
          (active.data.length === 0 ? (
            <OrdersEmpty message={emptyMessage} />
          ) : (
            <OrdersTable
              orders={active.data}
              onCancel={tab === 'open' ? handleCancel : undefined}
              cancellingId={cancellingId}
            />
          ))}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <RecentOrdersPreview />

        <section className="min-w-0 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
          <h2 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            Recent activity
          </h2>
          {dashboard.isPending && (
            <div className="h-40 animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
          )}
          {dashboard.isError && (
            <p className="text-sm text-[var(--color-text-secondary)]">
              Couldn&apos;t load recent activity.
            </p>
          )}
          {dashboard.data && <RecentActivity activity={dashboard.data.recentActivity} />}
        </section>
      </div>
    </div>
  )
}
