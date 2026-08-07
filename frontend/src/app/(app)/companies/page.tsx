'use client'

import { useMemo, useState } from 'react'
import { useCompanies } from '@/hooks/useCompanies'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { listSectors } from '@/lib/graphql/companies'
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
  const sectors = useMemo(() => listSectors(), [])

  const {
    data: companies,
    isPending,
    isError,
    refetch,
  } = useCompanies({
    search: debouncedSearch,
    sector,
  })

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
      {!isPending && !isError && companies && companies.length === 0 && <CompanyListEmpty />}
      {!isPending && !isError && companies && companies.length > 0 && (
        <CompanyTable companies={companies} />
      )}
    </div>
  )
}
