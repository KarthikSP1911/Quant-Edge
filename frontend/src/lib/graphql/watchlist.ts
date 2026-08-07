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
    }
  }
`

interface WatchlistQueryResponse {
  watchlist: { company: Company; addedAt: string }[]
}

export async function fetchWatchlist(): Promise<Company[]> {
  const data = await graphqlRequest<WatchlistQueryResponse>(WATCHLIST_QUERY)
  return data.watchlist.map((item) => item.company)
}

export function addToWatchlist(symbol: string): Promise<void> {
  return apiRequest<void>(`/api/watchlist/${symbol}`, { method: 'POST' })
}

export function removeFromWatchlist(symbol: string): Promise<void> {
  return apiRequest<void>(`/api/watchlist/${symbol}`, { method: 'DELETE' })
}
