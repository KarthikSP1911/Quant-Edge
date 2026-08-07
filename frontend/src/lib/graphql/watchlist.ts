// MOCK — the `watchlists` GraphQL query/mutation doesn't exist yet (see backend/docs/api-contract.md:
// it lands in a future slice once the `watchlists` table is wired up). There is no real REST/GraphQL
// endpoint to add/remove a watched symbol yet, so removal below is a local-only mutation against this
// in-memory list (no network call, no persistence across reloads).
//
// Company data is NOT duplicated here — symbols reference `fetchCompanies` in `./companies.ts` so
// price/name/sector stay internally consistent with the rest of the app.

import { fetchCompanies } from './companies'
import type { Company } from '@/types/company'

// Symbols the mock "current user" is watching. Cross-referenced against MOCK_COMPANIES via
// fetchCompanies so this file never re-states company data.
const WATCHED_SYMBOLS = ['AAPL', 'NVDA', 'TSLA', 'AMZN', 'JPM', 'DIS']

export async function fetchWatchlist(): Promise<Company[]> {
  await new Promise((resolve) => setTimeout(resolve, 250))

  const allCompanies = await fetchCompanies({})
  const bySymbol = new Map(allCompanies.map((company) => [company.symbol, company]))

  return WATCHED_SYMBOLS.map((symbol) => bySymbol.get(symbol)).filter(
    (company): company is Company => company !== undefined,
  )
}

export async function removeFromWatchlist(symbol: string): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 150))

  const index = WATCHED_SYMBOLS.indexOf(symbol)
  if (index !== -1) {
    WATCHED_SYMBOLS.splice(index, 1)
  }
}
