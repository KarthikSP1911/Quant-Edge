'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchStockDetail } from '@/lib/graphql/stockDetail'
import type { ChartRange } from '@/types/stock'

export function useStockDetail(symbol: string, range: ChartRange) {
  return useQuery({
    queryKey: ['stockDetail', symbol, range],
    queryFn: () => fetchStockDetail(symbol, range),
  })
}
