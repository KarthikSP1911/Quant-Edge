import type { Company } from '@/types/company'
import { graphqlRequest } from './client'
import { apiRequest } from '@/lib/api/client'

const WATCHLIST_QUERY = /* GraphQL */ `
  query Watchlist {
    watchlist {
      company {
        id
        symbol
        name
        sector
        industry
        description
        logoUrl
        exchange
      }
      addedAt
      quote {
        currentPrice
      }
    }
  }
`

export interface WatchlistCompany extends Company {
  currentPrice: number
}

interface WatchlistQueryResponse {
  watchlist: { company: Company; addedAt: string; quote: { currentPrice: number } }[]
}

export async function fetchWatchlist(): Promise<WatchlistCompany[]> {
  const data = await graphqlRequest<WatchlistQueryResponse>(WATCHLIST_QUERY)
  return data.watchlist.map((item) => ({ ...item.company, currentPrice: item.quote.currentPrice }))
}

export function addToWatchlist(symbol: string): Promise<void> {
  return apiRequest<void>(`/api/watchlist/${symbol}`, { method: 'POST' })
}

export function removeFromWatchlist(symbol: string): Promise<void> {
  return apiRequest<void>(`/api/watchlist/${symbol}`, { method: 'DELETE' })
}
