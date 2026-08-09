'use client'

import Link from 'next/link'
import type { Holding } from '@/types/portfolio'
import { formatChangePercent, formatPrice, formatSignedCurrency } from '@/lib/utils/format'
import CompanyLogo from '@/components/companies/CompanyLogo'

function ChangeBadge({ value, formatted }: { value: number; formatted: string }) {
  const positive = value >= 0
  return (
    <span className={positive ? 'text-[var(--color-profit)]' : 'text-[var(--color-loss)]'}>
      {formatted}
    </span>
  )
}

function HoldingIdentity({ holding }: { holding: Holding }) {
  return (
    <div className="flex items-center gap-3">
      <CompanyLogo symbol={holding.symbol} logoUrl={holding.logoUrl} />
      <div className="min-w-0">
        <div className="font-medium text-[var(--color-text-primary)]">{holding.symbol}</div>
        <div className="truncate text-xs text-[var(--color-text-secondary)]">{holding.name}</div>
      </div>
    </div>
  )
}

export default function HoldingsTable({
  holdings,
  limit,
}: {
  holdings: Holding[]
  limit?: number
}) {
  const visible = limit ? holdings.slice(0, limit) : holdings

  return (
    <>
      {/* Desktop / tablet table */}
      <div className="hidden overflow-x-auto rounded-lg border border-[var(--color-border)] sm:block">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-[var(--color-border)] bg-[var(--color-card-bg)] text-xs text-[var(--color-text-secondary)]">
              <th className="px-4 py-3 font-medium">Company</th>
              <th className="px-4 py-3 text-right font-medium">Qty</th>
              <th className="px-4 py-3 text-right font-medium">Avg Cost</th>
              <th className="px-4 py-3 text-right font-medium">Price</th>
              <th className="px-4 py-3 text-right font-medium">Market Value</th>
              <th className="px-4 py-3 text-right font-medium">Unrealized P&L</th>
              <th className="px-4 py-3 text-right font-medium">Day Change</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((holding) => (
              <tr
                key={holding.symbol}
                className="border-b border-[var(--color-border)] last:border-b-0"
              >
                <td className="px-4 py-3">
                  <Link
                    href={`/stocks/${holding.symbol}`}
                    className="block rounded-sm transition-opacity hover:opacity-80"
                  >
                    <HoldingIdentity holding={holding} />
                  </Link>
                </td>
                <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                  {holding.quantity}
                </td>
                <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                  {formatPrice(holding.averageCost)}
                </td>
                <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                  {formatPrice(holding.currentPrice)}
                </td>
                <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                  {formatPrice(holding.marketValue)}
                </td>
                <td className="px-4 py-3 text-right">
                  <ChangeBadge
                    value={holding.gainLoss}
                    formatted={`${formatSignedCurrency(holding.gainLoss)} (${formatChangePercent(holding.gainLossPercent)})`}
                  />
                </td>
                <td className="px-4 py-3 text-right">
                  <ChangeBadge
                    value={holding.changePercent}
                    formatted={formatChangePercent(holding.changePercent)}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile card list */}
      <div className="flex flex-col gap-2 sm:hidden">
        {visible.map((holding) => (
          <Link
            key={holding.symbol}
            href={`/stocks/${holding.symbol}`}
            className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-3 transition-colors hover:bg-[var(--color-sidebar-hover)]"
          >
            <div className="flex items-center justify-between gap-3">
              <HoldingIdentity holding={holding} />
              <div className="shrink-0 text-right">
                <div className="font-medium text-[var(--color-text-primary)]">
                  {formatPrice(holding.marketValue)}
                </div>
                <div className="text-xs">
                  <ChangeBadge
                    value={holding.gainLoss}
                    formatted={`${formatSignedCurrency(holding.gainLoss)} (${formatChangePercent(holding.gainLossPercent)})`}
                  />
                </div>
              </div>
            </div>
            <div className="mt-2 flex justify-between text-xs text-[var(--color-text-secondary)]">
              <span>
                {holding.quantity} {holding.quantity === 1 ? 'share' : 'shares'} @{' '}
                {formatPrice(holding.averageCost)}
              </span>
              <span>
                Day:{' '}
                <ChangeBadge
                  value={holding.changePercent}
                  formatted={formatChangePercent(holding.changePercent)}
                />
              </span>
            </div>
          </Link>
        ))}
      </div>
    </>
  )
}
