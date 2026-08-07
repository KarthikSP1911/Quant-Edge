export function DashboardSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div
            key={i}
            className="flex flex-col gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5"
          >
            <div className="h-3 w-24 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
            <div className="h-7 w-32 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          </div>
        ))}
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
          <div className="mb-4 h-4 w-40 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          <div className="h-4 w-full animate-pulse rounded-full bg-[var(--color-sidebar-hover)]" />
          <div className="mt-4 space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div
                key={i}
                className="h-4 w-full animate-pulse rounded bg-[var(--color-sidebar-hover)]"
              />
            ))}
          </div>
        </div>
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
          <div className="mb-4 h-4 w-40 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          <div className="space-y-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="h-5 w-full animate-pulse rounded bg-[var(--color-sidebar-hover)]"
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

export function DashboardError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[var(--color-loss)]/40 bg-red-50/40 py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">Couldn&apos;t load dashboard</p>
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
