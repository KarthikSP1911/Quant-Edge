import type { Company } from '@/types/company'
import { graphqlRequest } from './client'

const COMPANIES_QUERY = /* GraphQL */ `
  query Companies {
    companies {
      id
      symbol
      name
      sector
      industry
      description
      logoUrl
      exchange
    }
  }
`

export async function fetchCompanies(): Promise<Company[]> {
  const data = await graphqlRequest<{ companies: Company[] }>(COMPANIES_QUERY)
  return data.companies
}
