import type { RecentActivity as RecentActivityItem } from '@/lib/graphql/dashboard'
import { formatPrice } from '@/lib/utils/format'

function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export default function RecentActivity({ activity }: { activity: RecentActivityItem[] }) {
  if (activity.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-[var(--color-border)] py-12 text-center">
        <p className="text-sm font-medium text-[var(--color-text-primary)]">No activity yet</p>
        <p className="text-xs text-[var(--color-text-secondary)]">
          Your buy and sell orders will show up here.
        </p>
      </div>
    )
  }

  return (
    <ul className="flex flex-col">
      {activity.map((item) => {
        const isBuy = item.type === 'BUY'
        return (
          <li
            key={item.id}
            className="flex items-center justify-between gap-3 border-b border-[var(--color-border)] py-3 last:border-b-0"
          >
            <div className="flex items-center gap-3">
              <span
                className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                  isBuy
                    ? 'bg-[var(--color-profit)]/10 text-[var(--color-profit)]'
                    : 'bg-[var(--color-loss)]/10 text-[var(--color-loss)]'
                }`}
                aria-hidden
              >
                {isBuy ? 'B' : 'S'}
              </span>
              <div className="flex flex-col">
                <span className="text-sm font-medium text-[var(--color-text-primary)]">
                  {isBuy ? 'Bought' : 'Sold'} {item.symbol}
                </span>
                <span className="text-xs text-[var(--color-text-secondary)]">
                  {formatTimestamp(item.timestamp)}
                </span>
              </div>
            </div>
            <div className="flex flex-col items-end tabular-nums">
              <span className="text-sm font-medium text-[var(--color-text-primary)]">
                {item.quantity} {item.quantity === 1 ? 'share' : 'shares'} @{' '}
                {formatPrice(item.price)}
              </span>
              <span className="text-xs text-[var(--color-text-secondary)]">
                {formatPrice(item.quantity * item.price)}
              </span>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
