// Mirrors the `portfolios` table (backend/docs/api-contract.md, Phase 2 — not yet defined).
// See MOCK note in lib/graphql/portfolio.ts for why this is client-computed for now.
export interface Holding {
  symbol: string
  name: string
  sector: string
  logoUrl: string | null
  quantity: number
  avgCost: number
  currentPrice: number
  changePercent: number | null
}

export interface PortfolioSummary {
  cashBalance: number
  totalMarketValue: number
  totalCostBasis: number
  totalUnrealizedPnl: number
  totalUnrealizedPnlPercent: number
}

export interface Portfolio {
  summary: PortfolioSummary
  holdings: Holding[]
}
