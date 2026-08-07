// MOCK — the `portfolio` GraphQL query doesn't exist yet (see backend/docs/api-contract.md:
// it lands in slice "phase-2/graphql-setup-and-company-queries" alongside `companies`, but
// portfolio reads are a separate slice that hasn't been scoped yet). The `portfolios` table
// stores holdings (symbol, quantity, avg_cost) — current price/change come from the same
// Redis-backed live-quote source as the companies list, so holdings here cross-reference
// `MOCK_COMPANIES` via `fetchCompanies` instead of duplicating price data.
//
// Starting cash balance is framed by CLAUDE.md as $10,000; cash below is $10,000 minus what's
// "already invested" (quantity * avgCost) in the mock holdings, so the summary numbers are
// internally consistent.

import type { Holding, Portfolio } from '@/types/portfolio'
import { fetchCompanies } from './companies'

const STARTING_CASH_BALANCE = 10_000

// symbol -> [quantity, avgCost]. avgCost is intentionally offset from the current mock price
// (some above, some below) so both gain and loss states render in the UI.
const MOCK_LOTS: Record<string, [quantity: number, avgCost: number]> = {
  AAPL: [4, 195.1],
  MSFT: [1, 402.75],
  NVDA: [3, 118.4],
  AMZN: [2, 178.6],
  TSLA: [1, 268.3],
  JPM: [2, 205.9],
  KO: [10, 68.75],
  AMD: [2, 172.85],
}

export async function fetchPortfolio(): Promise<Portfolio> {
  await new Promise((resolve) => setTimeout(resolve, 250))

  const companies = await fetchCompanies({})
  const companyBySymbol = new Map(companies.map((company) => [company.symbol, company]))

  const holdings: Holding[] = Object.entries(MOCK_LOTS)
    .map(([symbol, [quantity, avgCost]]) => {
      const company = companyBySymbol.get(symbol)
      if (!company || company.price === null) return null
      const holding: Holding = {
        symbol: company.symbol,
        name: company.name,
        sector: company.sector,
        logoUrl: company.logoUrl,
        quantity,
        avgCost,
        currentPrice: company.price,
        changePercent: company.changePercent,
      }
      return holding
    })
    .filter((holding): holding is Holding => holding !== null)

  const totalCostBasis = holdings.reduce((sum, h) => sum + h.quantity * h.avgCost, 0)
  const totalMarketValue = holdings.reduce((sum, h) => sum + h.quantity * h.currentPrice, 0)
  const totalUnrealizedPnl = totalMarketValue - totalCostBasis
  const totalUnrealizedPnlPercent =
    totalCostBasis === 0 ? 0 : (totalUnrealizedPnl / totalCostBasis) * 100

  const cashBalance = STARTING_CASH_BALANCE - totalCostBasis

  return {
    summary: {
      cashBalance,
      totalMarketValue,
      totalCostBasis,
      totalUnrealizedPnl,
      totalUnrealizedPnlPercent,
    },
    holdings,
  }
}
