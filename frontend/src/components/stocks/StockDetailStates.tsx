export function StockDetailSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start gap-4">
        <div className="h-9 w-9 animate-pulse rounded-full bg-[var(--color-sidebar-hover)]" />
        <div className="space-y-2">
          <div className="h-4 w-32 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
          <div className="h-3 w-48 animate-pulse rounded bg-[var(--color-sidebar-hover)]" />
        </div>
      </div>
      <div className="h-[360px] w-full animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
    </div>
  )
}

export function StockDetailError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[var(--color-loss)]/40 bg-red-50/40 py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">Couldn&apos;t load this stock</p>
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

export function StockNotFound({ symbol }: { symbol: string }) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">
        No stock found for &quot;{symbol}&quot;
      </p>
      <p className="text-sm text-[var(--color-text-secondary)]">Check the symbol and try again.</p>
    </div>
  )
}
