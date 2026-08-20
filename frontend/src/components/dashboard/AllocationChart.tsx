'use client'

import { useState } from 'react'
import type { SectorAllocation } from '@/lib/graphql/dashboard'
import { formatPrice } from '@/lib/utils/format'

// Categorical palette validated for the light chart surface via the dataviz skill's
// scripts/validate_palette.js (7-slot subset, worst adjacent CVD ΔE 9.1, worst normal-vision
// ΔE 19.6). Fixed hue order — never cycled or reassigned by sort/filter. Slots that land below
// 3:1 contrast against the card surface (aqua, yellow, magenta) carry the "relief" mitigation:
// every segment gets a visible direct label, not color alone, and the legend always renders.
const SECTOR_COLORS = [
  '#2a78d6', // blue
  '#eb6834', // orange
  '#1baf7a', // aqua
  '#eda100', // yellow
  '#e87ba4', // magenta
  '#4a3aa7', // violet
  '#008300', // green
]

const SIZE = 100
const CENTER = SIZE / 2
const RADIUS = 39
const STROKE_WIDTH = 16
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const GAP = 1.5 // viewBox units — reads as ~2px surface gap at typical render size

interface Segment {
  sector: string
  value: number
  percent: number
  color: string
  dashLength: number
  dashOffset: number
}

function buildSegments(allocation: SectorAllocation[]): Segment[] {
  let cumulative = 0
  return allocation.map((a, i) => {
    const length = (a.percent / 100) * CIRCUMFERENCE
    const segment: Segment = {
      sector: a.sector,
      value: a.value,
      percent: a.percent,
      color: SECTOR_COLORS[i % SECTOR_COLORS.length],
      dashLength: Math.max(length - GAP, 0),
      dashOffset: -cumulative,
    }
    cumulative += length
    return segment
  })
}

export default function AllocationChart({ allocation }: { allocation: SectorAllocation[] }) {
  const [activeIndex, setActiveIndex] = useState<number | null>(null)

  if (allocation.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-[var(--color-border)] py-12 text-center">
        <p className="text-sm font-medium text-[var(--color-text-primary)]">No holdings yet</p>
        <p className="text-xs text-[var(--color-text-secondary)]">
          Buy a stock to see your sector allocation here.
        </p>
      </div>
    )
  }

  const segments = buildSegments(allocation)
  const total = allocation.reduce((sum, a) => sum + a.value, 0)
  const active = activeIndex !== null ? segments[activeIndex] : null

  return (
    <div className="flex flex-col gap-6 sm:flex-row sm:items-center sm:gap-8">
      {/* Donut — one hue per sector, 2px surface gap between segments so adjacent
          low-contrast colors (aqua/yellow/magenta) never touch edge-to-edge. */}
      <div className="mx-auto w-48 shrink-0">
        <div className="relative aspect-square w-48">
          <svg
            viewBox={`0 0 ${SIZE} ${SIZE}`}
            className="h-full w-full overflow-visible"
            role="img"
            aria-label={`Portfolio allocation by sector: ${allocation
              .map((a) => `${a.sector} ${a.percent.toFixed(1)}%`)
              .join(', ')}`}
          >
            <circle
              cx={CENTER}
              cy={CENTER}
              r={RADIUS}
              fill="none"
              stroke="var(--color-sidebar-hover)"
              strokeWidth={STROKE_WIDTH}
            />
            {segments
              .map((s, i) => ({ s, i }))
              // Paint the hovered/focused segment last so its wider stroke isn't
              // clipped by a neighbor painted on top of it at their shared boundary.
              .sort((a, b) => Number(a.i === activeIndex) - Number(b.i === activeIndex))
              .map(({ s, i }) => (
                <circle
                  key={s.sector}
                  cx={CENTER}
                  cy={CENTER}
                  r={RADIUS}
                  fill="none"
                  stroke={s.color}
                  strokeWidth={activeIndex === i ? STROKE_WIDTH + 3 : STROKE_WIDTH}
                  strokeDasharray={`${s.dashLength} ${CIRCUMFERENCE - s.dashLength}`}
                  strokeDashoffset={s.dashOffset}
                  transform={`rotate(-90 ${CENTER} ${CENTER})`}
                  style={{
                    pointerEvents: 'stroke',
                    cursor: 'pointer',
                    transition: 'stroke-width 120ms',
                  }}
                  tabIndex={0}
                  onMouseEnter={() => setActiveIndex(i)}
                  onMouseLeave={() => setActiveIndex(null)}
                  onFocus={() => setActiveIndex(i)}
                  onBlur={() => setActiveIndex(null)}
                >
                  <title>{`${s.sector}: ${formatPrice(s.value)} (${s.percent.toFixed(1)}%)`}</title>
                </circle>
              ))}
          </svg>

          {/* Center readout — swaps to the hovered/focused sector, defaults to the total */}
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-0.5 text-center">
            <span className="text-xs text-[var(--color-text-secondary)]">
              {active ? active.sector : 'Total'}
            </span>
            <span className="text-base font-semibold text-[var(--color-text-primary)]">
              {formatPrice(active ? active.value : total)}
            </span>
            {active && (
              <span className="text-xs text-[var(--color-text-secondary)]">
                {active.percent.toFixed(1)}%
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Legend with direct labels — required relief for the sub-3:1 slots, and per the
          dataviz skill identity must never rely on color alone. */}
      <ul className="flex min-w-0 flex-1 flex-col gap-2">
        {segments.map((s, i) => (
          <li
            key={s.sector}
            className={`flex items-center justify-between gap-3 rounded-md px-1.5 py-0.5 text-sm transition-colors ${
              activeIndex === i ? 'bg-[var(--color-sidebar-hover)]' : ''
            }`}
            onMouseEnter={() => setActiveIndex(i)}
            onMouseLeave={() => setActiveIndex(null)}
          >
            <span className="flex min-w-0 items-center gap-2">
              <span
                className="h-2.5 w-2.5 shrink-0 rounded-full"
                style={{ backgroundColor: s.color }}
                aria-hidden
              />
              <span className="truncate text-[var(--color-text-primary)]">{s.sector}</span>
            </span>
            <span className="flex shrink-0 items-center gap-3 tabular-nums">
              <span className="text-[var(--color-text-secondary)]">{formatPrice(s.value)}</span>
              <span className="w-12 text-right font-medium text-[var(--color-text-primary)]">
                {s.percent.toFixed(1)}%
              </span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
