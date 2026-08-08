'use client'

import Link from 'next/link'
import CompanyLogo from '@/components/companies/CompanyLogo'
import {
  formatChangePercent,
  formatMarketCap,
  formatPeRatio,
  formatPrice,
} from '@/lib/utils/format'
import type { ComparisonEntry } from '@/types/comparison'

/**
 * The rows are declared once and rendered for every stock, so each column shows exactly the same
 * metrics in the same order — that alignment is what makes the table scannable across stocks. Each
 * accessor returns a preformatted string; the format helpers already render null as an em dash.
 */
const ROWS: { label: string; value: (entry: ComparisonEntry) => string }[] = [
  { label: 'Price', value: (entry) => formatPrice(entry.price) },
  { label: 'Change', value: (entry) => formatChangePercent(entry.changePercent) },
  { label: 'Market cap', value: (entry) => formatMarketCap(entry.fundamentals.marketCap) },
  { label: 'P/E ratio', value: (entry) => formatPeRatio(entry.fundamentals.peRatio) },
  { label: '52w high', value: (entry) => formatPrice(entry.fundamentals.fiftyTwoWeekHigh) },
  { label: '52w low', value: (entry) => formatPrice(entry.fundamentals.fiftyTwoWeekLow) },
  { label: 'Sector', value: (entry) => entry.sector },
  { label: 'Exchange', value: (entry) => entry.exchange },
]

function changeClass(changePercent: number): string {
  if (changePercent > 0) return 'text-[var(--color-profit)]'
  if (changePercent < 0) return 'text-[var(--color-loss)]'
  return 'text-[var(--color-text-secondary)]'
}

export default function FundamentalsTable({ entries }: { entries: ComparisonEntry[] }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-[var(--color-border)]">
      <table className="w-full text-left text-sm">
        <caption className="sr-only">Fundamentals compared across selected stocks</caption>
        <thead>
          <tr className="border-b border-[var(--color-border)] bg-[var(--color-card-bg)]">
            <th
              scope="col"
              className="px-4 py-3 text-xs font-medium text-[var(--color-text-secondary)]"
            >
              Metric
            </th>
            {entries.map((entry) => (
              <th key={entry.symbol} scope="col" className="px-4 py-3 font-medium">
                <Link
                  href={`/stocks/${entry.symbol}`}
                  className="flex items-center gap-3 rounded-sm transition-opacity hover:opacity-80"
                >
                  <CompanyLogo symbol={entry.symbol} logoUrl={entry.logoUrl} />
                  <span className="min-w-0">
                    <span className="block text-[var(--color-text-primary)]">{entry.symbol}</span>
                    <span className="block truncate text-xs font-normal text-[var(--color-text-secondary)]">
                      {entry.name}
                    </span>
                  </span>
                </Link>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {ROWS.map((row) => (
            <tr key={row.label} className="border-b border-[var(--color-border)] last:border-b-0">
              <th
                scope="row"
                className="px-4 py-3 text-xs font-medium text-[var(--color-text-secondary)]"
              >
                {row.label}
              </th>
              {entries.map((entry) => (
                <td
                  key={entry.symbol}
                  className={`px-4 py-3 tabular-nums ${
                    row.label === 'Change'
                      ? changeClass(entry.changePercent)
                      : 'text-[var(--color-text-primary)]'
                  }`}
                >
                  {row.value(entry)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
