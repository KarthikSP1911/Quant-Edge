export function TimeMachineSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4"
          >
            <div className="h-2.5 w-20 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
            <div className="mt-3 h-5 w-24 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          </div>
        ))}
      </div>
      <div className="overflow-hidden rounded-lg border border-[var(--color-border)]">
        {Array.from({ length: 4 }).map((_, i) => (
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
    </div>
  )
}

export function TimeMachineEmpty() {
  return (
    <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">No holdings on this date</p>
      <p className="text-sm text-[var(--color-text-secondary)]">
        You hadn&apos;t bought anything yet as of the selected date.
      </p>
    </div>
  )
}

export function TimeMachineError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[var(--color-loss)]/40 bg-red-50/40 py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">Couldn&apos;t load this date</p>
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
