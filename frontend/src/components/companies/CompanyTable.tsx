'use client'

import Link from 'next/link'
import type { Company } from '@/types/company'
import {
  formatChangePercent,
  formatMarketCap,
  formatPeRatio,
  formatPrice,
} from '@/lib/utils/format'
import CompanyLogo from './CompanyLogo'

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

export default function CompanyTable({ companies }: { companies: Company[] }) {
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
              <th className="px-4 py-3 text-right font-medium">Market Cap</th>
              <th className="px-4 py-3 text-right font-medium">P/E</th>
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
                <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                  {formatMarketCap(company.marketCap)}
                </td>
                <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                  {formatPeRatio(company.peRatio)}
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
            className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-3 transition-colors hover:bg-[var(--color-sidebar-hover)]"
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
            <div className="mt-2 flex justify-between text-xs text-[var(--color-text-secondary)]">
              <span>{company.sector}</span>
              <span>{formatMarketCap(company.marketCap)} cap</span>
            </div>
          </Link>
        ))}
      </div>
    </>
  )
}
