'use client'

import { useMemo, useState } from 'react'
import { useCompanies } from '@/hooks/useCompanies'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import CompanyFilters from '@/components/companies/CompanyFilters'
import CompanyTable from '@/components/companies/CompanyTable'
import {
  CompanyListEmpty,
  CompanyListError,
  CompanyTableSkeleton,
} from '@/components/companies/CompanyListStates'

export default function CompaniesPage() {
  const [search, setSearch] = useState('')
  const [sector, setSector] = useState('All')
  const debouncedSearch = useDebouncedValue(search, 300)

  const { data: companies, isPending, isError, refetch } = useCompanies()

  const sectors = useMemo(
    () => Array.from(new Set((companies ?? []).map((company) => company.sector))).sort(),
    [companies],
  )

  const filteredCompanies = useMemo(() => {
    if (!companies) return []
    const query = debouncedSearch.trim().toLowerCase()
    return companies.filter((company) => {
      const matchesSector = sector === 'All' || company.sector === sector
      const matchesSearch =
        !query ||
        company.symbol.toLowerCase().includes(query) ||
        company.name.toLowerCase().includes(query)
      return matchesSector && matchesSearch
    })
  }, [companies, debouncedSearch, sector])

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Companies</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Browse and research companies across sectors.
        </p>
      </div>

      <CompanyFilters
        search={search}
        onSearchChange={setSearch}
        sector={sector}
        onSectorChange={setSector}
        sectors={sectors}
      />

      {isPending && <CompanyTableSkeleton />}
      {isError && <CompanyListError onRetry={() => refetch()} />}
      {!isPending && !isError && filteredCompanies.length === 0 && <CompanyListEmpty />}
      {!isPending && !isError && filteredCompanies.length > 0 && (
        <CompanyTable companies={filteredCompanies} />
      )}
    </div>
  )
}
