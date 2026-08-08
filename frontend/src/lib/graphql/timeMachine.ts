import type {
  TimeMachineDecision,
  TimeMachineHolding,
  TimeMachineResult,
} from '@/types/timeMachine'
import { graphqlRequest } from './client'

const TIME_MACHINE_QUERY = /* GraphQL */ `
  query PortfolioTimeMachine($asOfDate: String!) {
    portfolioTimeMachine(asOfDate: $asOfDate) {
      asOfDate
      cashBalance
      totalMarketValue
      totalAccountValue
      holdings {
        company {
          symbol
          name
          sector
          logoUrl
        }
        quantity
        averageCost
        priceAtDate
        marketValue
        gainLoss
        gainLossPercent
      }
      bestDecisions {
        symbol
        quantity
        buyPrice
        sellPrice
        executedAt
        realizedGainPercent
      }
      worstDecisions {
        symbol
        quantity
        buyPrice
        sellPrice
        executedAt
        realizedGainPercent
      }
    }
  }
`

interface RawHolding {
  company: { symbol: string; name: string; sector: string; logoUrl: string | null }
  quantity: number
  averageCost: number
  priceAtDate: number
  marketValue: number
  gainLoss: number
  gainLossPercent: number
}

interface RawDecision {
  symbol: string
  quantity: number
  buyPrice: number
  sellPrice: number
  executedAt: string
  realizedGainPercent: number
}

interface TimeMachineQueryResponse {
  portfolioTimeMachine: {
    asOfDate: string
    cashBalance: number
    totalMarketValue: number
    totalAccountValue: number
    holdings: RawHolding[]
    bestDecisions: RawDecision[]
    worstDecisions: RawDecision[]
  }
}

function toHolding(holding: RawHolding): TimeMachineHolding {
  return {
    symbol: holding.company.symbol,
    name: holding.company.name,
    sector: holding.company.sector,
    logoUrl: holding.company.logoUrl,
    quantity: holding.quantity,
    averageCost: holding.averageCost,
    priceAtDate: holding.priceAtDate,
    marketValue: holding.marketValue,
    gainLoss: holding.gainLoss,
    gainLossPercent: holding.gainLossPercent,
  }
}

function toDecision(decision: RawDecision): TimeMachineDecision {
  return { ...decision }
}

export async function fetchPortfolioTimeMachine(asOfDate: string): Promise<TimeMachineResult> {
  const data = await graphqlRequest<TimeMachineQueryResponse>(TIME_MACHINE_QUERY, { asOfDate })
  const result = data.portfolioTimeMachine

  return {
    asOfDate: result.asOfDate,
    cashBalance: result.cashBalance,
    totalMarketValue: result.totalMarketValue,
    totalAccountValue: result.totalAccountValue,
    holdings: result.holdings.map(toHolding),
    bestDecisions: result.bestDecisions.map(toDecision),
    worstDecisions: result.worstDecisions.map(toDecision),
  }
}
