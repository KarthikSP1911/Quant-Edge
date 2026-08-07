'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchCompanies, type CompanyFilters } from '@/lib/graphql/companies'

export function useCompanies(filters: CompanyFilters) {
  return useQuery({
    queryKey: ['companies', filters],
    queryFn: () => fetchCompanies(filters),
  })
}
