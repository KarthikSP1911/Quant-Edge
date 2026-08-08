'use client'

import { useState } from 'react'
import { useActivityTimeline } from '@/hooks/useActivityTimeline'
import ActivityFilters, { type ActivityFiltersValue } from '@/components/activity/ActivityFilters'
import ActivityTimelineList from '@/components/activity/ActivityTimelineList'
import {
  ActivityEmpty,
  ActivityError,
  ActivityListSkeleton,
} from '@/components/activity/ActivityStates'

const PAGE_SIZE = 20

const EMPTY_FILTERS: ActivityFiltersValue = {
  action: '',
  entityType: '',
  startDate: '',
  endDate: '',
}

function toStartOfDayInstant(date: string): string | null {
  return date ? `${date}T00:00:00Z` : null
}

function toEndOfDayInstant(date: string): string | null {
  return date ? `${date}T23:59:59Z` : null
}

export default function ActivityPage() {
  const [filters, setFilters] = useState<ActivityFiltersValue>(EMPTY_FILTERS)
  const [page, setPage] = useState(0)

  const query = useActivityTimeline({
    action: filters.action || null,
    entityType: filters.entityType || null,
    startDate: toStartOfDayInstant(filters.startDate),
    endDate: toEndOfDayInstant(filters.endDate),
    page,
    size: PAGE_SIZE,
  })

  const handleFiltersChange = (next: ActivityFiltersValue) => {
    setFilters(next)
    setPage(0)
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Activity</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Every trade, order, and watchlist change on your account.
        </p>
      </div>

      <ActivityFilters value={filters} onChange={handleFiltersChange} />

      {query.isPending && <ActivityListSkeleton />}
      {query.isError && <ActivityError onRetry={() => query.refetch()} />}
      {!query.isPending &&
        !query.isError &&
        query.data &&
        (query.data.content.length === 0 ? (
          <ActivityEmpty />
        ) : (
          <>
            <ActivityTimelineList entries={query.data.content} />
            <div className="flex items-center justify-between text-sm text-[var(--color-text-secondary)]">
              <span>
                Page {query.data.page + 1} of {Math.max(query.data.totalPages, 1)} ·{' '}
                {query.data.totalElements} total
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(p - 1, 0))}
                  className="rounded-md border border-[var(--color-border)] px-3 py-1.5 font-medium text-[var(--color-text-primary)] transition-colors hover:bg-[var(--color-sidebar-hover)] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  type="button"
                  disabled={page + 1 >= query.data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                  className="rounded-md border border-[var(--color-border)] px-3 py-1.5 font-medium text-[var(--color-text-primary)] transition-colors hover:bg-[var(--color-sidebar-hover)] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        ))}
    </div>
  )
}
