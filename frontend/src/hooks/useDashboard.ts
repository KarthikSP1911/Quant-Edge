'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchDashboardSummary } from '@/lib/graphql/dashboard'

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: fetchDashboardSummary,
  })
}
