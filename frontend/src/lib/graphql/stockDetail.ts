// MOCK — no `stock`/`chart` GraphQL query or OHLCV/news/financials data source exists in
// docs/api-contract.md yet. Chart data will likely come from a Redis-backed quote/candle cache
// per CLAUDE.md's data strategy; shape here is a best guess and WILL need reconciling once that
// contract lands. Company profile fields reuse the real `companies` columns where available.

import type { Candle, ChartRange, NewsItem, StockDetail } from '@/types/stock'
import { fetchCompanies } from './companies'

const CANDLES_PER_RANGE: Record<ChartRange, number> = {
  '1D': 78, // 5-minute bars over a 6.5h session
  '1W': 5,
  '1M': 22,
  '3M': 66,
  '6M': 132,
  '1Y': 252,
}

// Simple seeded PRNG so mock candles are stable across renders/tests instead of re-randomizing.
function mulberry32(seed: number) {
  return function () {
    seed |= 0
    seed = (seed + 0x6d2b79f5) | 0
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function hashSymbol(symbol: string): number {
  let hash = 0
  for (let i = 0; i < symbol.length; i++) {
    hash = (hash << 5) - hash + symbol.charCodeAt(i)
    hash |= 0
  }
  return hash
}

function generateCandles(symbol: string, range: ChartRange, endPrice: number): Candle[] {
  const count = CANDLES_PER_RANGE[range]
  const rand = mulberry32(hashSymbol(symbol) + count)
  const isIntraday = range === '1D'

  let price = endPrice * (1 - (rand() - 0.5) * 0.15)
  const candles: Candle[] = []
  const now = new Date()

  for (let i = 0; i < count; i++) {
    const open = price
    const drift = (rand() - 0.48) * price * 0.02
    const close = Math.max(open + drift, 0.5)
    const high = Math.max(open, close) + rand() * price * 0.008
    const low = Math.min(open, close) - rand() * price * 0.008
    const volume = Math.round(1_000_000 + rand() * 8_000_000)

    const date = new Date(now)
    if (isIntraday) {
      date.setMinutes(date.getMinutes() - (count - i) * 5)
    } else {
      date.setDate(date.getDate() - (count - i))
    }

    candles.push({
      // Full ISO timestamp (not just the date) — the 1D range produces several bars per day,
      // and lightweight-charts requires strictly ascending, non-duplicate time values.
      time: date.toISOString(),
      open: Number(open.toFixed(2)),
      high: Number(high.toFixed(2)),
      low: Number(low.toFixed(2)),
      close: Number(close.toFixed(2)),
      volume,
    })

    price = close
  }

  // Force the last close to match the quoted price so the chart lines up with the header.
  candles[candles.length - 1].close = endPrice
  return candles
}

function generateNews(symbol: string, name: string): NewsItem[] {
  const templates = [
    `${name} beats quarterly earnings expectations`,
    `Analysts raise price target on ${symbol} after strong guidance`,
    `${name} announces new product roadmap`,
    `${symbol} shares react to sector-wide rally`,
    `${name} expands operations amid growing demand`,
  ]
  return templates.map((headline, i) => ({
    id: `${symbol}-news-${i}`,
    headline,
    source: ['Reuters', 'Bloomberg', 'CNBC', 'MarketWatch', 'The Wall Street Journal'][i],
    publishedAt: new Date(Date.now() - i * 6 * 60 * 60 * 1000).toISOString(),
    url: '#',
  }))
}

export async function fetchStockDetail(symbol: string): Promise<StockDetail | null> {
  await new Promise((resolve) => setTimeout(resolve, 250))

  const companies = await fetchCompanies({})
  const company = companies.find((c) => c.symbol === symbol.toUpperCase())
  if (!company || company.price === null) return null

  return {
    symbol: company.symbol,
    name: company.name,
    sector: company.sector,
    industry: company.industry,
    description:
      company.description ??
      `${company.name} operates in the ${company.industry} industry within the ${company.sector} sector, listed on ${company.exchange}.`,
    logoUrl: company.logoUrl,
    exchange: company.exchange,
    price: company.price,
    changePercent: company.changePercent ?? 0,
    marketCap: company.marketCap ?? 0,
    peRatio: company.peRatio,
    financials: {
      revenueTtm: company.marketCap ? company.marketCap * 0.18 : 0,
      epsTtm: company.peRatio ? company.price / company.peRatio : 0,
      dividendYield: ['JNJ', 'PG', 'KO', 'XOM', 'JPM', 'WMT'].includes(company.symbol)
        ? Number((1 + (hashSymbol(company.symbol) % 300) / 100).toFixed(2))
        : null,
      week52High: Number((company.price * 1.28).toFixed(2)),
      week52Low: Number((company.price * 0.72).toFixed(2)),
    },
    news: generateNews(company.symbol, company.name),
  }
}

export async function fetchCandles(
  symbol: string,
  range: ChartRange,
  price: number,
): Promise<Candle[]> {
  await new Promise((resolve) => setTimeout(resolve, 150))
  return generateCandles(symbol, range, price)
}
