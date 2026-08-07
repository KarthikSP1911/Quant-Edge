export function CompanyTableSkeleton() {
  return (
    <div className="overflow-hidden rounded-lg border border-[var(--color-border)]">
      {Array.from({ length: 8 }).map((_, i) => (
        <div
          key={i}
          className="flex items-center gap-3 border-b border-[var(--color-border)] px-4 py-3 last:border-b-0"
        >
          <div className="h-9 w-9 shrink-0 animate-pulse rounded-full bg-[var(--color-sidebar-hover)]" />
          <div className="flex-1 space-y-2">
            <div className="h-3 w-24 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
            <div className="h-2.5 w-40 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          </div>
          <div className="h-3 w-16 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
        </div>
      ))}
    </div>
  )
}

export function CompanyListEmpty() {
  return (
    <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">
        No companies match your filters
      </p>
      <p className="text-sm text-[var(--color-text-secondary)]">
        Try a different search term or sector.
      </p>
    </div>
  )
}

export function CompanyListError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[var(--color-loss)]/40 bg-red-50/40 py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">Couldn&apos;t load companies</p>
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
