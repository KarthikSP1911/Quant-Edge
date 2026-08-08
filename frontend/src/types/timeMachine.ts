// Mirrors the backend's `TimeMachineResult` GraphQL type
// (backend/src/main/resources/graphql/schema.graphqls).
export interface TimeMachineHolding {
  symbol: string
  name: string
  sector: string
  logoUrl: string | null
  quantity: number
  averageCost: number
  priceAtDate: number
  marketValue: number
  gainLoss: number
  gainLossPercent: number
}

export interface TimeMachineDecision {
  symbol: string
  quantity: number
  buyPrice: number
  sellPrice: number
  executedAt: string
  realizedGainPercent: number
}

export interface TimeMachineResult {
  asOfDate: string
  cashBalance: number
  holdings: TimeMachineHolding[]
  totalMarketValue: number
  totalAccountValue: number
  bestDecisions: TimeMachineDecision[]
  worstDecisions: TimeMachineDecision[]
}
