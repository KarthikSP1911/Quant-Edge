'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchActivityTimeline } from '@/lib/graphql/auditLog'
import type { ActivityTimelineFilters } from '@/types/auditLog'

export function useActivityTimeline(filters: ActivityTimelineFilters) {
  return useQuery({
    queryKey: ['activityTimeline', filters],
    queryFn: () => fetchActivityTimeline(filters),
  })
}
