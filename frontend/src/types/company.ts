// Mirrors the backend's `Company` GraphQL type (backend/src/main/resources/graphql/schema.graphqls).
export interface Company {
  id: string
  symbol: string
  name: string
  sector: string
  industry: string
  description: string | null
  logoUrl: string | null
  exchange: string
}
