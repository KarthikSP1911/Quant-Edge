// Mirrors the `companies` table (backend/docs/api-contract.md, Phase 2 slice 1).
// price/marketCap/peRatio are NOT backend fields yet — see MOCK note in lib/graphql/companies.ts.
export interface Company {
  id: string
  symbol: string
  name: string
  sector: string
  industry: string
  description: string | null
  logoUrl: string | null
  exchange: string
  price: number | null
  changePercent: number | null
  marketCap: number | null
  peRatio: number | null
}
