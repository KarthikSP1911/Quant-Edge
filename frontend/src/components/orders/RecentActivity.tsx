import type { RecentActivity as RecentActivityItem } from '@/lib/graphql/dashboard'
import { formatPrice } from '@/lib/utils/format'
import CompanyLogo from '@/components/companies/CompanyLogo'

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
            <div className="flex min-w-0 items-center gap-3">
              <div className="relative shrink-0">
                <CompanyLogo symbol={item.symbol} logoUrl={item.logoUrl} />
                <span
                  className={`absolute -bottom-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full border border-[var(--color-card-bg)] text-[9px] font-semibold ${
                    isBuy
                      ? 'bg-[var(--color-profit)]/10 text-[var(--color-profit)]'
                      : 'bg-[var(--color-loss)]/10 text-[var(--color-loss)]'
                  }`}
                  aria-hidden
                >
                  {isBuy ? 'B' : 'S'}
                </span>
              </div>
              <div className="flex min-w-0 flex-col">
                <span className="truncate text-sm font-medium text-[var(--color-text-primary)]">
                  {isBuy ? 'Bought' : 'Sold'} {item.symbol}
                </span>
                <span className="text-xs whitespace-nowrap text-[var(--color-text-secondary)]">
                  {formatTimestamp(item.timestamp)}
                </span>
              </div>
            </div>
            <div className="flex shrink-0 flex-col items-end tabular-nums">
              <span className="text-sm font-medium whitespace-nowrap text-[var(--color-text-primary)]">
                {item.quantity} {item.quantity === 1 ? 'share' : 'shares'} @{' '}
                {formatPrice(item.price)}
              </span>
              <span className="text-xs whitespace-nowrap text-[var(--color-text-secondary)]">
                {formatPrice(item.quantity * item.price)}
              </span>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
