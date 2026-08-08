import type { Candle, ChartRange, StockDetail } from '@/types/stock'
import { graphqlRequest } from './client'

// Twelve Data's `interval` param (see backend TwelveDataClient) doesn't have a "range" concept —
// the UI's range selector maps to an explicit (interval, outputSize) pair per range.
export const RANGE_TO_QUERY_ARGS: Record<ChartRange, { interval: string; outputSize: number }> = {
  '1D': { interval: '5min', outputSize: 78 }, // ~6.5h session / 5-minute bars
  '1W': { interval: '1h', outputSize: 35 },
  '1M': { interval: '1day', outputSize: 22 },
  '3M': { interval: '1day', outputSize: 66 },
  '6M': { interval: '1day', outputSize: 132 },
  '1Y': { interval: '1week', outputSize: 52 },
}

const STOCK_DETAIL_QUERY = /* GraphQL */ `
  query StockDetail($symbol: String!, $interval: String, $outputSize: Int) {
    stockDetail(symbol: $symbol, interval: $interval, outputSize: $outputSize) {
      company {
        symbol
        name
        sector
        industry
        description
        logoUrl
        exchange
      }
      quote {
        currentPrice
        previousClose
      }
      candles {
        datetime
        open
        high
        low
        close
        volume
      }
    }
  }
`

interface RawCandle {
  datetime: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

interface RawStockDetail {
  company: {
    symbol: string
    name: string
    sector: string
    industry: string
    description: string | null
    logoUrl: string | null
    exchange: string
  }
  quote: {
    currentPrice: number
    previousClose: number
  }
  candles: RawCandle[]
}

interface StockDetailQueryResponse {
  stockDetail: RawStockDetail | null
}

function toCandle(raw: RawCandle): Candle {
  return {
    time: raw.datetime,
    open: raw.open,
    high: raw.high,
    low: raw.low,
    close: raw.close,
    volume: raw.volume,
  }
}

export async function fetchStockDetail(
  symbol: string,
  range: ChartRange,
): Promise<StockDetail | null> {
  const { interval, outputSize } = RANGE_TO_QUERY_ARGS[range]
  const data = await graphqlRequest<StockDetailQueryResponse>(STOCK_DETAIL_QUERY, {
    symbol,
    interval,
    outputSize,
  })

  if (!data.stockDetail) return null
  const { company, quote, candles } = data.stockDetail

  const changePercent =
    quote.previousClose === 0
      ? 0
      : ((quote.currentPrice - quote.previousClose) / quote.previousClose) * 100

  return {
    symbol: company.symbol,
    name: company.name,
    sector: company.sector,
    industry: company.industry,
    description: company.description,
    logoUrl: company.logoUrl,
    exchange: company.exchange,
    price: quote.currentPrice,
    previousClose: quote.previousClose,
    changePercent,
    candles: candles.map(toCandle),
  }
}
