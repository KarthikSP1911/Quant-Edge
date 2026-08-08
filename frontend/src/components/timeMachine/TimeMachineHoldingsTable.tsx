import Link from 'next/link'
import type { TimeMachineHolding } from '@/types/timeMachine'
import { formatChangePercent, formatPrice, formatSignedCurrency } from '@/lib/utils/format'
import CompanyLogo from '@/components/companies/CompanyLogo'

export default function TimeMachineHoldingsTable({ holdings }: { holdings: TimeMachineHolding[] }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-[var(--color-border)]">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-[var(--color-border)] bg-[var(--color-card-bg)] text-xs text-[var(--color-text-secondary)]">
            <th className="px-4 py-3 font-medium">Company</th>
            <th className="px-4 py-3 text-right font-medium">Qty</th>
            <th className="px-4 py-3 text-right font-medium">Avg Cost</th>
            <th className="px-4 py-3 text-right font-medium">Price at Date</th>
            <th className="px-4 py-3 text-right font-medium">Market Value</th>
            <th className="px-4 py-3 text-right font-medium">Gain/Loss</th>
          </tr>
        </thead>
        <tbody>
          {holdings.map((holding) => (
            <tr
              key={holding.symbol}
              className="border-b border-[var(--color-border)] last:border-b-0"
            >
              <td className="px-4 py-3">
                <Link
                  href={`/stocks/${holding.symbol}`}
                  className="flex items-center gap-3 rounded-sm transition-opacity hover:opacity-80"
                >
                  <CompanyLogo symbol={holding.symbol} logoUrl={holding.logoUrl} />
                  <div className="min-w-0">
                    <div className="font-medium text-[var(--color-text-primary)]">
                      {holding.symbol}
                    </div>
                    <div className="truncate text-xs text-[var(--color-text-secondary)]">
                      {holding.name}
                    </div>
                  </div>
                </Link>
              </td>
              <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                {holding.quantity}
              </td>
              <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                {formatPrice(holding.averageCost)}
              </td>
              <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                {formatPrice(holding.priceAtDate)}
              </td>
              <td className="px-4 py-3 text-right text-[var(--color-text-primary)]">
                {formatPrice(holding.marketValue)}
              </td>
              <td className="px-4 py-3 text-right">
                <span
                  className={
                    holding.gainLoss >= 0
                      ? 'text-[var(--color-profit)]'
                      : 'text-[var(--color-loss)]'
                  }
                >
                  {formatSignedCurrency(holding.gainLoss)} (
                  {formatChangePercent(holding.gainLossPercent)})
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
