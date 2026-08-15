'use client'

import { useMemo, useState } from 'react'
import { useCompanies } from '@/hooks/useCompanies'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { MAX_COMPARE_SYMBOLS } from '@/types/comparison'

export default function SymbolPicker({
  selected,
  onChange,
}: {
  selected: string[]
  onChange: (symbols: string[]) => void
}) {
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebouncedValue(search, 200)
  const { data: companies } = useCompanies()

  const atCapacity = selected.length >= MAX_COMPARE_SYMBOLS

  const matches = useMemo(() => {
    const query = debouncedSearch.trim().toLowerCase()
    if (!query || !companies) return []
    return companies
      .filter(
        (company) =>
          !selected.includes(company.symbol) &&
          (company.symbol.toLowerCase().includes(query) ||
            company.name.toLowerCase().includes(query)),
      )
      .slice(0, 6)
  }, [companies, debouncedSearch, selected])

  function add(symbol: string) {
    if (atCapacity || selected.includes(symbol)) return
    onChange([...selected, symbol])
    setSearch('')
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        {selected.map((symbol) => (
          <span
            key={symbol}
            className="flex items-center gap-2 rounded-full bg-[var(--color-accent-light)] py-1 pr-1 pl-3 text-sm font-medium text-[var(--color-accent-blue)]"
          >
            {symbol}
            <button
              type="button"
              onClick={() => onChange(selected.filter((s) => s !== symbol))}
              aria-label={`Remove ${symbol} from comparison`}
              className="flex h-5 w-5 items-center justify-center rounded-full transition-colors hover:bg-[var(--color-accent-blue)] hover:text-white"
            >
              &times;
            </button>
          </span>
        ))}
      </div>

      <div className="relative max-w-md">
        <input
          type="text"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          disabled={atCapacity}
          placeholder={
            atCapacity
              ? `Remove a stock to add another (max ${MAX_COMPARE_SYMBOLS})`
              : 'Add a stock by symbol or name…'
          }
          aria-label="Search stocks to compare"
          className="w-full rounded-md border border-[var(--color-border)] bg-[var(--color-card-bg)] px-3 py-2 text-sm text-[var(--color-text-primary)] transition-colors outline-none placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-accent-blue)] disabled:bg-[var(--color-sidebar-hover)] disabled:text-[var(--color-text-muted)]"
        />

        {matches.length > 0 && (
          <ul className="absolute z-20 mt-1 w-full overflow-hidden rounded-md border border-[var(--color-border)] bg-[var(--color-card-bg)] shadow-lg">
            {matches.map((company) => (
              <li key={company.id}>
                <button
                  type="button"
                  onClick={() => add(company.symbol)}
                  className="flex w-full items-baseline gap-2 px-3 py-2 text-left text-sm transition-colors hover:bg-[var(--color-sidebar-hover)]"
                >
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {company.symbol}
                  </span>
                  <span className="truncate text-xs text-[var(--color-text-secondary)]">
                    {company.name}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
