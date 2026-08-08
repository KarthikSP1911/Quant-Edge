'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchPortfolioTimeMachine } from '@/lib/graphql/timeMachine'

export function useTimeMachine(asOfDate: string) {
  return useQuery({
    queryKey: ['timeMachine', asOfDate],
    queryFn: () => fetchPortfolioTimeMachine(asOfDate),
  })
}
