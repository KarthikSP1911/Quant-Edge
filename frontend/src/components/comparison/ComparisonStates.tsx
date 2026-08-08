import { MIN_COMPARE_SYMBOLS } from '@/types/comparison'

export function ComparisonSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="h-64 w-full animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
      <div className="h-[380px] w-full animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
    </div>
  )
}

export function ComparisonError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-[var(--color-loss)]/40 bg-red-50/40 py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">
        Couldn&apos;t load this comparison
      </p>
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

export function NeedsMoreStocks({ selectedCount }: { selectedCount: number }) {
  const remaining = MIN_COMPARE_SYMBOLS - selectedCount
  return (
    <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">
        Add {remaining} more {remaining === 1 ? 'stock' : 'stocks'} to compare
      </p>
      <p className="text-sm text-[var(--color-text-secondary)]">
        Pick at least {MIN_COMPARE_SYMBOLS} stocks to see them side by side.
      </p>
    </div>
  )
}

export function NoOverlappingHistory() {
  return (
    <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-12 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">No overlapping price history</p>
      <p className="text-sm text-[var(--color-text-secondary)]">
        These stocks share no common bars over this range, so the overlay can&apos;t be plotted. Try
        a longer range.
      </p>
    </div>
  )
}
