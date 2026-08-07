'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchPortfolio } from '@/lib/graphql/portfolio'

export function usePortfolio() {
  return useQuery({
    queryKey: ['portfolio'],
    queryFn: fetchPortfolio,
  })
}
