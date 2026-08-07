// Mirrors the backend's `PortfolioSummary`/`PortfolioPosition` GraphQL types
// (backend/src/main/resources/graphql/schema.graphqls).
export interface Holding {
  symbol: string
  name: string
  sector: string
  logoUrl: string | null
  quantity: number
  averageCost: number
  currentPrice: number
  changePercent: number
  marketValue: number
  gainLoss: number
  gainLossPercent: number
}

export interface PortfolioSummary {
  cashBalance: number
  totalMarketValue: number
  totalAccountValue: number
  totalCostBasis: number
  totalUnrealizedPnl: number
  totalUnrealizedPnlPercent: number
}

export interface Portfolio {
  summary: PortfolioSummary
  holdings: Holding[]
}
