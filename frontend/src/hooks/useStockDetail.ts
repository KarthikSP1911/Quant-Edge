'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchCandles, fetchStockDetail } from '@/lib/graphql/stockDetail'
import type { ChartRange } from '@/types/stock'

export function useStockDetail(symbol: string) {
  return useQuery({
    queryKey: ['stockDetail', symbol],
    queryFn: () => fetchStockDetail(symbol),
  })
}

export function useCandles(symbol: string, range: ChartRange, price: number | undefined) {
  return useQuery({
    queryKey: ['candles', symbol, range],
    queryFn: () => fetchCandles(symbol, range, price!),
    enabled: price !== undefined,
  })
}
