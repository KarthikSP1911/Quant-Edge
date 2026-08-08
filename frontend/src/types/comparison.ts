// Mirrors the backend's `StockComparison` GraphQL type
// (backend/src/main/resources/graphql/schema.graphqls).
import type { Candle } from './stock'

/**
 * Every metric is nullable. The comparison table renders the same rows for every stock, so a metric
 * the provider has no data for renders as an em dash rather than dropping the row or showing 0.
 */
export interface Fundamentals {
  marketCap: number | null
  peRatio: number | null
  fiftyTwoWeekHigh: number | null
  fiftyTwoWeekLow: number | null
}

export interface ComparisonEntry {
  symbol: string
  name: string
  sector: string
  exchange: string
  logoUrl: string | null
  price: number
  previousClose: number
  changePercent: number
  fundamentals: Fundamentals
  /** Already aligned by the backend: same length, same datetimes, oldest-first, across all entries. */
  candles: Candle[]
}

export interface StockComparison {
  entries: ComparisonEntry[]
}

export const MIN_COMPARE_SYMBOLS = 2
export const MAX_COMPARE_SYMBOLS = 3
