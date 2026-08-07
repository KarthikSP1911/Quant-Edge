import type { Financials } from '@/types/stock'
import { formatMarketCap, formatPrice } from '@/lib/utils/format'

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between border-b border-[var(--color-border)] py-2.5 last:border-b-0">
      <span className="text-sm text-[var(--color-text-secondary)]">{label}</span>
      <span className="text-sm font-medium text-[var(--color-text-primary)]">{value}</span>
    </div>
  )
}

export default function FinancialsTable({ financials }: { financials: Financials }) {
  return (
    <div>
      <Row label="Revenue (TTM)" value={formatMarketCap(financials.revenueTtm)} />
      <Row label="EPS (TTM)" value={formatPrice(financials.epsTtm)} />
      <Row
        label="Dividend Yield"
        value={financials.dividendYield !== null ? `${financials.dividendYield.toFixed(2)}%` : '—'}
      />
      <Row label="52-Week High" value={formatPrice(financials.week52High)} />
      <Row label="52-Week Low" value={formatPrice(financials.week52Low)} />
    </div>
  )
}
