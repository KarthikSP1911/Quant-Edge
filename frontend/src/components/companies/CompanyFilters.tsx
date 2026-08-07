'use client'

interface CompanyFiltersProps {
  search: string
  onSearchChange: (value: string) => void
  sector: string
  onSectorChange: (value: string) => void
  sectors: string[]
}

export default function CompanyFilters({
  search,
  onSearchChange,
  sector,
  onSectorChange,
  sectors,
}: CompanyFiltersProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="relative flex-1">
        <svg
          className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-[var(--color-text-muted)]"
          viewBox="0 0 16 16"
          fill="none"
          aria-hidden="true"
        >
          <circle cx="7" cy="7" r="5.5" stroke="currentColor" strokeWidth="1.3" />
          <path
            d="M11.5 11.5L14.5 14.5"
            stroke="currentColor"
            strokeWidth="1.3"
            strokeLinecap="round"
          />
        </svg>
        <input
          type="text"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search by symbol or company name"
          aria-label="Search companies"
          className="w-full rounded-md border border-[var(--color-border)] bg-white py-2 pr-3 pl-9 text-sm text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-accent-blue)] focus:ring-1 focus:ring-[var(--color-accent-blue)] focus:outline-none"
        />
      </div>

      <select
        value={sector}
        onChange={(e) => onSectorChange(e.target.value)}
        aria-label="Filter by sector"
        className="rounded-md border border-[var(--color-border)] bg-white px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:ring-1 focus:ring-[var(--color-accent-blue)] focus:outline-none sm:w-56"
      >
        <option value="All">All Sectors</option>
        {sectors.map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </select>
    </div>
  )
}
