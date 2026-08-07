// MOCK — the `dashboard` GraphQL query doesn't exist yet (see backend/docs/api-contract.md:
// no dashboard/portfolio slice has landed). There is also no `portfolios`/`transactions` data
// yet to read from. Holdings quantities and recent activity below are invented, but every
// symbol/price is cross-referenced from `fetchCompanies` in `lib/graphql/companies.ts` so the
// numbers stay internally consistent with the rest of the app until the real contract lands.
//
// Starting balance follows CLAUDE.md's "AI-powered stock research, quantified" simulated-trading
// framing: every new account starts with $10,000 in cash.

import { fetchCompanies } from '@/lib/graphql/companies'
import type { Company } from '@/types/company'

const STARTING_CASH_BALANCE = 10000

export interface Holding {
  symbol: string
  quantity: number
  company: Company
  marketValue: number
  dayChangeValue: number
}

export interface SectorAllocation {
  sector: string
  value: number
  percent: number
}

export interface RecentActivity {
  id: string
  type: 'BUY' | 'SELL'
  symbol: string
  quantity: number
  price: number
  timestamp: string
}

export interface DashboardSummary {
  cashBalance: number
  holdings: Holding[]
  holdingsValue: number
  netWorth: number
  dayChangeValue: number
  dayChangePercent: number
  allocation: SectorAllocation[]
  recentActivity: RecentActivity[]
}

// symbol -> shares held, invented for the mock account
const MOCK_HOLDING_QUANTITIES: Record<string, number> = {
  AAPL: 12,
  MSFT: 4,
  NVDA: 8,
  AMZN: 6,
  JPM: 10,
  V: 5,
  KO: 20,
}

// Recent activity, most-recent first. Prices/symbols must exist in MOCK_COMPANIES.
const MOCK_RECENT_ACTIVITY: Omit<RecentActivity, 'id'>[] = [
  { type: 'BUY', symbol: 'NVDA', quantity: 2, price: 133.1, timestamp: '2026-08-07T13:42:00Z' },
  { type: 'SELL', symbol: 'TSLA', quantity: 3, price: 251.4, timestamp: '2026-08-06T19:05:00Z' },
  { type: 'BUY', symbol: 'AAPL', quantity: 5, price: 224.8, timestamp: '2026-08-06T14:22:00Z' },
  { type: 'BUY', symbol: 'KO', quantity: 20, price: 70.95, timestamp: '2026-08-04T15:10:00Z' },
  { type: 'SELL', symbol: 'AMD', quantity: 4, price: 161.2, timestamp: '2026-08-03T18:47:00Z' },
  { type: 'BUY', symbol: 'V', quantity: 5, price: 308.6, timestamp: '2026-08-01T16:30:00Z' },
  { type: 'BUY', symbol: 'JPM', quantity: 10, price: 215.9, timestamp: '2026-07-30T14:05:00Z' },
  { type: 'BUY', symbol: 'MSFT', quantity: 4, price: 418.25, timestamp: '2026-07-28T13:55:00Z' },
]

export async function fetchDashboardSummary(): Promise<DashboardSummary> {
  await new Promise((resolve) => setTimeout(resolve, 250))

  const companies = await fetchCompanies({})
  const companyBySymbol = new Map(companies.map((company) => [company.symbol, company]))

  const holdings: Holding[] = Object.entries(MOCK_HOLDING_QUANTITIES)
    .map(([symbol, quantity]) => {
      const company = companyBySymbol.get(symbol)
      if (!company || company.price === null) return null
      const marketValue = company.price * quantity
      const changePercent = company.changePercent ?? 0
      // dayChangeValue derived from today's move: value now minus value at yesterday's close
      const priorClosePrice = company.price / (1 + changePercent / 100)
      const dayChangeValue = (company.price - priorClosePrice) * quantity
      return { symbol, quantity, company, marketValue, dayChangeValue }
    })
    .filter((holding): holding is Holding => holding !== null)

  const holdingsValue = holdings.reduce((sum, holding) => sum + holding.marketValue, 0)
  const dayChangeValue = holdings.reduce((sum, holding) => sum + holding.dayChangeValue, 0)
  const priorHoldingsValue = holdingsValue - dayChangeValue
  const dayChangePercent = priorHoldingsValue > 0 ? (dayChangeValue / priorHoldingsValue) * 100 : 0

  const netWorth = STARTING_CASH_BALANCE + holdingsValue

  const bySector = new Map<string, number>()
  for (const holding of holdings) {
    bySector.set(
      holding.company.sector,
      (bySector.get(holding.company.sector) ?? 0) + holding.marketValue,
    )
  }
  const allocation: SectorAllocation[] = Array.from(bySector.entries())
    .map(([sector, value]) => ({
      sector,
      value,
      percent: holdingsValue > 0 ? (value / holdingsValue) * 100 : 0,
    }))
    .sort((a, b) => b.value - a.value)

  const recentActivity: RecentActivity[] = MOCK_RECENT_ACTIVITY.map((activity, index) => ({
    id: `${activity.symbol}-${index}`,
    ...activity,
  }))

  return {
    cashBalance: STARTING_CASH_BALANCE,
    holdings,
    holdingsValue,
    netWorth,
    dayChangeValue,
    dayChangePercent,
    allocation,
    recentActivity,
  }
}
