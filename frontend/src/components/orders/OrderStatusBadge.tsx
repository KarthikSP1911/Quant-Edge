import type { OrderStatus } from '@/types/order'

const STATUS_STYLES: Record<OrderStatus, string> = {
  FILLED: 'bg-[var(--color-profit)]/10 text-[var(--color-profit)]',
  PARTIALLY_FILLED: 'bg-[var(--color-profit)]/10 text-[var(--color-profit)]',
  PENDING: 'bg-[var(--color-warning)]/10 text-[var(--color-warning)]',
  OPEN: 'bg-[var(--color-warning)]/10 text-[var(--color-warning)]',
  CANCELLED: 'bg-[var(--color-text-muted)]/10 text-[var(--color-text-muted)]',
  EXPIRED: 'bg-[var(--color-text-muted)]/10 text-[var(--color-text-muted)]',
  REJECTED: 'bg-[var(--color-loss)]/10 text-[var(--color-loss)]',
}

const STATUS_LABELS: Record<OrderStatus, string> = {
  FILLED: 'Filled',
  PARTIALLY_FILLED: 'Partially filled',
  PENDING: 'Pending',
  OPEN: 'Open',
  CANCELLED: 'Cancelled',
  EXPIRED: 'Expired',
  REJECTED: 'Rejected',
}

export default function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {STATUS_LABELS[status]}
    </span>
  )
}
