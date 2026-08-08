export function OrdersTableSkeleton() {
  return (
    <div className="overflow-hidden rounded-lg border border-[var(--color-border)]">
      {Array.from({ length: 5 }).map((_, i) => (
        <div
          key={i}
          className="flex items-center gap-4 border-b border-[var(--color-border)] px-4 py-3 last:border-b-0"
        >
          <div className="h-3 w-16 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          <div className="h-3 w-12 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          <div className="h-3 w-20 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          <div className="ml-auto h-5 w-20 animate-pulse rounded-full bg-[var(--color-sidebar-hover)]" />
        </div>
      ))}
    </div>
  )
}

export function OrdersEmpty({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">{message}</p>
    </div>
  )
}

export function OrdersError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[var(--color-loss)]/40 bg-red-50/40 py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">Couldn&apos;t load orders</p>
      <p className="text-sm text-[var(--color-text-secondary)]">
        Something went wrong. Please try again.
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
      >
        Retry
      </button>
    </div>
  )
}
