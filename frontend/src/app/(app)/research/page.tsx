'use client'

import { Suspense, useState } from 'react'
import { useSearchParams } from 'next/navigation'
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
import ResearchAgentInline from '@/components/research/ResearchAgentInline'
import ResearchNotesTab from '@/components/research/ResearchNotesTab'

type Tab = 'ai' | 'compare' | 'notes'

const tabs: { id: Tab; label: string }[] = [
  { id: 'ai', label: 'AI Research' },
  { id: 'compare', label: 'Compare' },
  { id: 'notes', label: 'Notes' },
]

function isTab(value: string | null): value is Tab {
  return value === 'ai' || value === 'compare' || value === 'notes'
}

function CompareTab() {
  const [symbols, setSymbols] = useState<string[]>([])
  const [range, setRange] = useState<ChartRange>('6M')

  const { data: comparison, isPending, isError, refetch } = useComparison(symbols, range)

  const enoughStocks = symbols.length >= MIN_COMPARE_SYMBOLS
  const entries = comparison?.entries ?? []
  const hasOverlap = entries.length > 0 && entries[0].candles.length > 0

  return (
    <div className="flex flex-col gap-6">
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

function ResearchPageContent() {
  const searchParams = useSearchParams()
  const initialTab = searchParams.get('tab')
  const [tab, setTab] = useState<Tab>(isTab(initialTab) ? initialTab : 'ai')

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Research</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Ask the AI research agent about a stock, compare a few stocks side by side, or read saved
          research notes.
        </p>
      </header>

      <div className="flex gap-1 border-b border-[var(--color-border)]">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
              tab === t.id
                ? 'border-[var(--color-accent-blue)] text-[var(--color-accent-blue)]'
                : 'border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'ai' && <ResearchAgentInline />}
      {tab === 'compare' && <CompareTab />}
      {tab === 'notes' && <ResearchNotesTab />}
    </div>
  )
}

export default function ResearchPage() {
  return (
    <Suspense fallback={null}>
      <ResearchPageContent />
    </Suspense>
  )
}
