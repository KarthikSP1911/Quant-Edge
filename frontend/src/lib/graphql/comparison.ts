import type { ChartRange } from '@/types/stock'
import type { ComparisonEntry, StockComparison } from '@/types/comparison'
import { graphqlRequest } from './client'
import { RANGE_TO_QUERY_ARGS } from './stockDetail'

const STOCK_COMPARISON_QUERY = /* GraphQL */ `
  query StockComparison($symbols: [String!]!, $interval: String, $outputSize: Int) {
    stockComparison(symbols: $symbols, interval: $interval, outputSize: $outputSize) {
      entries {
        company {
          symbol
          name
          sector
          exchange
          logoUrl
        }
        quote {
          currentPrice
          previousClose
        }
        fundamentals {
          marketCap
          peRatio
          fiftyTwoWeekHigh
          fiftyTwoWeekLow
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
  }
`

interface RawEntry {
  company: {
    symbol: string
    name: string
    sector: string
    exchange: string
    logoUrl: string | null
  }
  quote: {
    currentPrice: number
    previousClose: number
  }
  fundamentals: {
    marketCap: number | null
    peRatio: number | null
    fiftyTwoWeekHigh: number | null
    fiftyTwoWeekLow: number | null
  }
  candles: {
    datetime: string
    open: number
    high: number
    low: number
    close: number
    volume: number
  }[]
}

interface StockComparisonQueryResponse {
  stockComparison: { entries: RawEntry[] }
}

function toEntry(raw: RawEntry): ComparisonEntry {
  const { company, quote, fundamentals, candles } = raw
  const changePercent =
    quote.previousClose === 0
      ? 0
      : ((quote.currentPrice - quote.previousClose) / quote.previousClose) * 100

  return {
    symbol: company.symbol,
    name: company.name,
    sector: company.sector,
    exchange: company.exchange,
    logoUrl: company.logoUrl,
    price: quote.currentPrice,
    previousClose: quote.previousClose,
    changePercent,
    fundamentals,
    candles: candles.map((candle) => ({
      time: candle.datetime,
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
      volume: candle.volume,
    })),
  }
}

export async function fetchStockComparison(
  symbols: string[],
  range: ChartRange,
): Promise<StockComparison> {
  const { interval, outputSize } = RANGE_TO_QUERY_ARGS[range]
  const data = await graphqlRequest<StockComparisonQueryResponse>(STOCK_COMPARISON_QUERY, {
    symbols,
    interval,
    outputSize,
  })

  return { entries: data.stockComparison.entries.map(toEntry) }
}
