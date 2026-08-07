'use client'

import Link from 'next/link'
import type { Company } from '@/types/company'
import { formatChangePercent, formatPrice } from '@/lib/utils/format'
import CompanyLogo from '@/components/companies/CompanyLogo'

function ChangeBadge({ changePercent }: { changePercent: number | null }) {
  if (changePercent === null) {
    return <span className="text-[var(--color-text-muted)]">—</span>
  }
  const positive = changePercent >= 0
  return (
    <span className={positive ? 'text-[var(--color-profit)]' : 'text-[var(--color-loss)]'}>
      {formatChangePercent(changePercent)}
    </span>
  )
}

function CompanyIdentity({ company }: { company: Company }) {
  return (
    <div className="flex items-center gap-3">
      <CompanyLogo symbol={company.symbol} logoUrl={company.logoUrl} />
      <div className="min-w-0">
        <div className="font-medium text-[var(--color-text-primary)]">{company.symbol}</div>
        <div className="truncate text-xs text-[var(--color-text-secondary)]">{company.name}</div>
      </div>
    </div>
  )
}

function RemoveButton({
  onRemove,
  isPending,
  className,
}: {
  onRemove: () => void
  isPending: boolean
  className?: string
}) {
  return (
    <button
      type="button"
      disabled={isPending}
      onClick={(e) => {
        e.preventDefault()
        e.stopPropagation()
        onRemove()
      }}
      className={`rounded-md border border-[var(--color-border)] px-3 py-1.5 text-xs font-medium text-[var(--color-text-secondary)] transition-colors hover:border-[var(--color-loss)] hover:text-[var(--color-loss)] disabled:cursor-not-allowed disabled:opacity-50 ${className ?? ''}`}
    >
      {isPending ? 'Removing…' : 'Remove'}
    </button>
  )
}

export default function WatchlistTable({
  companies,
  onRemove,
  removingSymbol,
}: {
  companies: Company[]
  onRemove: (symbol: string) => void
  removingSymbol: string | null
}) {
  return (
    <>
      {/* Desktop / tablet table */}
      <div className="hidden overflow-x-auto rounded-lg border border-[var(--color-border)] sm:block">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-[var(--color-border)] bg-[var(--color-card-bg)] text-xs text-[var(--color-text-secondary)]">
              <th className="px-4 py-3 font-medium">Company</th>
              <th className="px-4 py-3 font-medium">Sector</th>
              <th className="px-4 py-3 text-right font-medium">Price</th>
              <th className="px-4 py-3 text-right font-medium">Change</th>
              <th className="px-4 py-3 text-right font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {companies.map((company) => (
              <tr
                key={company.id}
                className="border-b border-[var(--color-border)] last:border-b-0"
              >
                <td className="px-4 py-3">
                  <Link
                    href={`/stocks/${company.symbol}`}
                    className="block rounded-sm transition-opacity hover:opacity-80"
                  >
                    <CompanyIdentity company={company} />
                  </Link>
                </td>
                <td className="px-4 py-3 text-[var(--color-text-secondary)]">{company.sector}</td>
                <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                  {formatPrice(company.price)}
                </td>
                <td className="px-4 py-3 text-right">
                  <ChangeBadge changePercent={company.changePercent} />
                </td>
                <td className="px-4 py-3 text-right">
                  <RemoveButton
                    onRemove={() => onRemove(company.symbol)}
                    isPending={removingSymbol === company.symbol}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile card list */}
      <div className="flex flex-col gap-2 sm:hidden">
        {companies.map((company) => (
          <Link
            key={company.id}
            href={`/stocks/${company.symbol}`}
            className="block rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-3 transition-colors hover:bg-[var(--color-sidebar-hover)]"
          >
            <div className="flex items-center justify-between gap-3">
              <CompanyIdentity company={company} />
              <div className="shrink-0 text-right">
                <div className="font-medium text-[var(--color-text-primary)]">
                  {formatPrice(company.price)}
                </div>
                <div className="text-xs">
                  <ChangeBadge changePercent={company.changePercent} />
                </div>
              </div>
            </div>
            <div className="mt-2 flex items-center justify-between text-xs text-[var(--color-text-secondary)]">
              <span>{company.sector}</span>
              <RemoveButton
                onRemove={() => onRemove(company.symbol)}
                isPending={removingSymbol === company.symbol}
              />
            </div>
          </Link>
        ))}
      </div>
    </>
  )
}
