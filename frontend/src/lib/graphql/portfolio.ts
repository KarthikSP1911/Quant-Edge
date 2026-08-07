import type { Holding, Portfolio } from '@/types/portfolio'
import { graphqlRequest } from './client'

const PORTFOLIO_QUERY = /* GraphQL */ `
  query Portfolio {
    portfolio {
      cashBalance
      totalMarketValue
      totalAccountValue
      positions {
        company {
          symbol
          name
          sector
          logoUrl
        }
        quantity
        averageCost
        currentPrice
        changePercent
        marketValue
        gainLoss
        gainLossPercent
      }
    }
  }
`

interface RawPosition {
  company: { symbol: string; name: string; sector: string; logoUrl: string | null }
  quantity: number
  averageCost: number
  currentPrice: number
  changePercent: number
  marketValue: number
  gainLoss: number
  gainLossPercent: number
}

interface PortfolioQueryResponse {
  portfolio: {
    cashBalance: number
    totalMarketValue: number
    totalAccountValue: number
    positions: RawPosition[]
  }
}

function toHolding(position: RawPosition): Holding {
  return {
    symbol: position.company.symbol,
    name: position.company.name,
    sector: position.company.sector,
    logoUrl: position.company.logoUrl,
    quantity: position.quantity,
    averageCost: position.averageCost,
    currentPrice: position.currentPrice,
    changePercent: position.changePercent,
    marketValue: position.marketValue,
    gainLoss: position.gainLoss,
    gainLossPercent: position.gainLossPercent,
  }
}

export async function fetchPortfolio(): Promise<Portfolio> {
  const data = await graphqlRequest<PortfolioQueryResponse>(PORTFOLIO_QUERY)
  const { cashBalance, totalMarketValue, totalAccountValue, positions } = data.portfolio

  const holdings = positions.map(toHolding)
  const totalUnrealizedPnl = holdings.reduce((sum, holding) => sum + holding.gainLoss, 0)
  const totalCostBasis = totalMarketValue - totalUnrealizedPnl
  const totalUnrealizedPnlPercent =
    totalCostBasis === 0 ? 0 : (totalUnrealizedPnl / totalCostBasis) * 100

  return {
    summary: {
      cashBalance,
      totalMarketValue,
      totalAccountValue,
      totalCostBasis,
      totalUnrealizedPnl,
      totalUnrealizedPnlPercent,
    },
    holdings,
  }
}
