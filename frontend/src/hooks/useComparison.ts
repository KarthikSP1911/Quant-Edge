'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchStockComparison } from '@/lib/graphql/comparison'
import { MIN_COMPARE_SYMBOLS } from '@/types/comparison'
import type { ChartRange } from '@/types/stock'

export function useComparison(symbols: string[], range: ChartRange) {
  return useQuery({
    queryKey: ['stockComparison', symbols, range],
    queryFn: () => fetchStockComparison(symbols, range),
    // Below two symbols there is nothing to compare — don't spend a request to be told so.
    enabled: symbols.length >= MIN_COMPARE_SYMBOLS,
  })
}
