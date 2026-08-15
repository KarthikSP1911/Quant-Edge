'use client'

import { useMemo, useState } from 'react'
import ResearchAgentBody from '@/components/research/ResearchAgentBody'
import EmptyState from '@/components/ui/EmptyState'
import CompanyLogo from '@/components/companies/CompanyLogo'
import { useCompanies } from '@/hooks/useCompanies'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'

export default function ResearchAgentInline() {
  const [input, setInput] = useState('')
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [activeSymbol, setActiveSymbol] = useState<string | null>(null)

  const { data: companies } = useCompanies()
  const debouncedInput = useDebouncedValue(input, 200)

  const suggestions = useMemo(() => {
    const query = debouncedInput.trim().toLowerCase()
    if (!query || !companies) return []
    return companies
      .filter(
        (company) =>
          company.symbol.toLowerCase().includes(query) ||
          company.name.toLowerCase().includes(query),
      )
      .slice(0, 6)
  }, [companies, debouncedInput])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = input.trim().toUpperCase()
    if (!trimmed) return
    setShowSuggestions(false)
    setActiveSymbol(trimmed)
  }

  const selectSuggestion = (symbol: string) => {
    setInput(symbol)
    setShowSuggestions(false)
    setActiveSymbol(symbol)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-5">
        <h2 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
          Start a research run
        </h2>
        <form onSubmit={handleSubmit} className="flex items-center gap-3">
          <div className="relative flex-1">
            <input
              type="text"
              value={input}
              onChange={(e) => {
                setInput(e.target.value)
                setShowSuggestions(true)
              }}
              onFocus={() => setShowSuggestions(true)}
              onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
              placeholder="Enter a ticker symbol, e.g. AAPL"
              autoComplete="off"
              className="w-full rounded-md border border-[var(--color-border)] bg-white px-4 py-2.5 text-sm text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-accent-blue)] focus:outline-none"
            />

            {showSuggestions && suggestions.length > 0 && (
              <ul className="absolute z-20 mt-1 w-full overflow-hidden rounded-md border border-[var(--color-border)] bg-[var(--color-card-bg)] shadow-lg">
                {suggestions.map((company) => (
                  <li key={company.id}>
                    <button
                      type="button"
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => selectSuggestion(company.symbol)}
                      className="flex w-full items-center gap-3 px-3 py-2 text-left text-sm transition-colors hover:bg-[var(--color-sidebar-hover)]"
                    >
                      <CompanyLogo symbol={company.symbol} logoUrl={company.logoUrl} />
                      <span className="flex min-w-0 items-baseline gap-2">
                        <span className="font-medium text-[var(--color-text-primary)]">
                          {company.symbol}
                        </span>
                        <span className="truncate text-xs text-[var(--color-text-secondary)]">
                          {company.name}
                        </span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <button
            type="submit"
            disabled={!input.trim()}
            className="shrink-0 rounded-md bg-[var(--color-accent-blue)] px-5 py-2.5 text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
          >
            Research
          </button>
        </form>
      </div>

      {!activeSymbol && (
        <EmptyState
          title="No research yet"
          message="Enter a ticker above to kick off an autonomous research run."
        />
      )}

      {activeSymbol && (
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4">
          <h2 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            Autonomous research: {activeSymbol}
          </h2>
          <ResearchAgentBody key={activeSymbol} symbol={activeSymbol} />
        </div>
      )}
    </div>
  )
}
