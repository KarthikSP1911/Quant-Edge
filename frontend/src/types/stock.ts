export type ChartRange = '1D' | '1W' | '1M' | '3M' | '6M' | '1Y'

export interface Candle {
  time: string // full ISO 8601 timestamp (not just a date — 1D range has multiple bars/day)
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface NewsItem {
  id: string
  headline: string
  source: string
  publishedAt: string // ISO timestamp
  url: string
}

export interface Financials {
  revenueTtm: number
  epsTtm: number
  dividendYield: number | null
  week52High: number
  week52Low: number
}

export interface StockDetail {
  symbol: string
  name: string
  sector: string
  industry: string
  description: string | null
  logoUrl: string | null
  exchange: string
  price: number
  changePercent: number
  marketCap: number
  peRatio: number | null
  financials: Financials
  news: NewsItem[]
}
