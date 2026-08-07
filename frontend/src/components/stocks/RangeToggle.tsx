'use client'

import type { ChartRange } from '@/types/stock'

const RANGES: ChartRange[] = ['1D', '1W', '1M', '3M', '6M', '1Y']

export default function RangeToggle({
  value,
  onChange,
}: {
  value: ChartRange
  onChange: (range: ChartRange) => void
}) {
  return (
    <div className="flex gap-1 rounded-md border border-[var(--color-border)] p-1">
      {RANGES.map((range) => (
        <button
          key={range}
          type="button"
          onClick={() => onChange(range)}
          className={`rounded px-2.5 py-1 text-xs font-medium transition-colors ${
            value === range
              ? 'bg-[var(--color-accent-blue)] text-white'
              : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-sidebar-hover)]'
          }`}
        >
          {range}
        </button>
      ))}
    </div>
  )
}
