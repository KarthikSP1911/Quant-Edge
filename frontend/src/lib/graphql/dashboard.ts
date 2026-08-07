import { graphqlRequest } from './client'

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
  holdingsValue: number
  netWorth: number
  dayChangeValue: number
  dayChangePercent: number
  allocation: SectorAllocation[]
  recentActivity: RecentActivity[]
}

const DASHBOARD_QUERY = /* GraphQL */ `
  query Dashboard {
    dashboard {
      portfolio {
        cashBalance
        totalMarketValue
        totalAccountValue
        positions {
          company {
            sector
          }
          quantity
          currentPrice
          previousClose
          marketValue
        }
      }
      recentTransactions {
        id
        symbol
        side
        quantity
        price
        executedAt
      }
    }
  }
`

interface RawPosition {
  company: { sector: string }
  quantity: number
  currentPrice: number
  previousClose: number
  marketValue: number
}

interface RawTransaction {
  id: string
  symbol: string
  side: 'BUY' | 'SELL'
  quantity: number
  price: number
  executedAt: string
}

interface DashboardQueryResponse {
  dashboard: {
    portfolio: {
      cashBalance: number
      totalMarketValue: number
      totalAccountValue: number
      positions: RawPosition[]
    }
    recentTransactions: RawTransaction[]
  }
}

function computeAllocation(positions: RawPosition[], totalMarketValue: number): SectorAllocation[] {
  const bySector = new Map<string, number>()
  for (const position of positions) {
    bySector.set(
      position.company.sector,
      (bySector.get(position.company.sector) ?? 0) + position.marketValue,
    )
  }
  return Array.from(bySector.entries())
    .map(([sector, value]) => ({
      sector,
      value,
      percent: totalMarketValue > 0 ? (value / totalMarketValue) * 100 : 0,
    }))
    .sort((a, b) => b.value - a.value)
}

export async function fetchDashboardSummary(): Promise<DashboardSummary> {
  const data = await graphqlRequest<DashboardQueryResponse>(DASHBOARD_QUERY)
  const { cashBalance, totalMarketValue, totalAccountValue, positions } = data.dashboard.portfolio

  const dayChangeValue = positions.reduce(
    (sum, position) => sum + (position.currentPrice - position.previousClose) * position.quantity,
    0,
  )
  const priorHoldingsValue = totalMarketValue - dayChangeValue
  const dayChangePercent = priorHoldingsValue > 0 ? (dayChangeValue / priorHoldingsValue) * 100 : 0

  const recentActivity: RecentActivity[] = data.dashboard.recentTransactions.map((transaction) => ({
    id: transaction.id,
    type: transaction.side,
    symbol: transaction.symbol,
    quantity: transaction.quantity,
    price: transaction.price,
    timestamp: transaction.executedAt,
  }))

  return {
    cashBalance,
    holdingsValue: totalMarketValue,
    netWorth: totalAccountValue,
    dayChangeValue,
    dayChangePercent,
    allocation: computeAllocation(positions, totalMarketValue),
    recentActivity,
  }
}
