'use client'

import { useState } from 'react'
import { useComparison } from '@/hooks/useComparison'
import { MIN_COMPARE_SYMBOLS } from '@/types/comparison'
import type { ChartRange } from '@/types/stock'
import RangeToggle from '@/components/stocks/RangeToggle'
import SymbolPicker from '@/components/comparison/SymbolPicker'
import FundamentalsTable from '@/components/comparison/FundamentalsTable'
import ComparisonOverlayChart from '@/components/comparison/ComparisonOverlayChart'
import {
  ComparisonError,
  ComparisonSkeleton,
  NeedsMoreStocks,
  NoOverlappingHistory,
} from '@/components/comparison/ComparisonStates'

export default function ComparePage() {
  const [symbols, setSymbols] = useState<string[]>([])
  const [range, setRange] = useState<ChartRange>('6M')

  const { data: comparison, isPending, isError, refetch } = useComparison(symbols, range)

  const enoughStocks = symbols.length >= MIN_COMPARE_SYMBOLS
  const entries = comparison?.entries ?? []
  // The backend intersects the candle axis, so an empty first series means the stocks share no bars.
  const hasOverlap = entries.length > 0 && entries[0].candles.length > 0

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Compare stocks</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Put two or three stocks side by side on the same fundamentals and the same price chart.
        </p>
      </header>

      <SymbolPicker selected={symbols} onChange={setSymbols} />

      {!enoughStocks && <NeedsMoreStocks selectedCount={symbols.length} />}

      {enoughStocks && isPending && <ComparisonSkeleton />}
      {enoughStocks && isError && <ComparisonError onRetry={() => refetch()} />}

      {enoughStocks && !isPending && !isError && entries.length > 0 && (
        <div className="flex flex-col gap-6">
          <FundamentalsTable entries={entries} />

          <section className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">
                Relative performance
              </h2>
              <RangeToggle value={range} onChange={setRange} />
            </div>

            {hasOverlap ? (
              <div className="rounded-lg border border-[var(--color-border)] p-2">
                <ComparisonOverlayChart entries={entries} />
              </div>
            ) : (
              <NoOverlappingHistory />
            )}
          </section>
        </div>
      )}
    </div>
  )
}
