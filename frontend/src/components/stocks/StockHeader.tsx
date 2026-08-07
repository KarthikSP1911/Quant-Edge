'use client'

import type { StockDetail } from '@/types/stock'
import {
  formatMarketCap,
  formatPeRatio,
  formatPrice,
  formatChangePercent,
} from '@/lib/utils/format'
import CompanyLogo from '@/components/companies/CompanyLogo'

export default function StockHeader({ stock }: { stock: StockDetail }) {
  const positive = stock.changePercent >= 0

  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
      <div className="flex items-start gap-4">
        <CompanyLogo symbol={stock.symbol} logoUrl={stock.logoUrl} />
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">
              {stock.symbol}
            </h1>
            <span className="text-sm text-[var(--color-text-muted)]">{stock.exchange}</span>
          </div>
          <p className="text-sm text-[var(--color-text-secondary)]">{stock.name}</p>
          <p className="mt-1 text-xs text-[var(--color-text-muted)]">
            {stock.sector} &middot; {stock.industry}
          </p>
        </div>
      </div>

      <div className="text-left sm:text-right">
        <div className="text-2xl font-semibold text-[var(--color-text-primary)]">
          {formatPrice(stock.price)}
        </div>
        <div
          className={
            positive ? 'text-sm text-[var(--color-profit)]' : 'text-sm text-[var(--color-loss)]'
          }
        >
          {formatChangePercent(stock.changePercent)}
        </div>
        <div className="mt-1 text-xs text-[var(--color-text-muted)]">
          Mkt Cap {formatMarketCap(stock.marketCap)} &middot; P/E {formatPeRatio(stock.peRatio)}
        </div>
      </div>
    </div>
  )
}
