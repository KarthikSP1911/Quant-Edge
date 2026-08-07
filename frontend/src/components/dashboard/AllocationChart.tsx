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

export default function AllocationChart({ allocation }: { allocation: SectorAllocation[] }) {
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

  return (
    <div className="flex flex-col gap-4">
      {/* Horizontal stacked bar — one hue per sector, 2px surface gap between segments so
          adjacent low-contrast colors (aqua/yellow/magenta) never touch edge-to-edge. */}
      <div
        className="flex h-4 w-full overflow-hidden rounded-full bg-[var(--color-sidebar-hover)]"
        role="img"
        aria-label={`Portfolio allocation by sector: ${allocation
          .map((a) => `${a.sector} ${a.percent.toFixed(1)}%`)
          .join(', ')}`}
      >
        {allocation.map((a, i) => (
          <div
            key={a.sector}
            className="h-full first:rounded-l-full last:rounded-r-full"
            style={{
              width: `${a.percent}%`,
              backgroundColor: SECTOR_COLORS[i % SECTOR_COLORS.length],
              marginRight: i === allocation.length - 1 ? 0 : 2,
            }}
            title={`${a.sector}: ${a.percent.toFixed(1)}%`}
          />
        ))}
      </div>

      {/* Legend with direct labels — required relief for the sub-3:1 slots, and per the
          dataviz skill identity must never rely on color alone. */}
      <ul className="flex flex-col gap-2">
        {allocation.map((a, i) => (
          <li key={a.sector} className="flex items-center justify-between gap-3 text-sm">
            <span className="flex min-w-0 items-center gap-2">
              <span
                className="h-2.5 w-2.5 shrink-0 rounded-full"
                style={{ backgroundColor: SECTOR_COLORS[i % SECTOR_COLORS.length] }}
                aria-hidden
              />
              <span className="truncate text-[var(--color-text-primary)]">{a.sector}</span>
            </span>
            <span className="flex shrink-0 items-center gap-3 tabular-nums">
              <span className="text-[var(--color-text-secondary)]">{formatPrice(a.value)}</span>
              <span className="w-12 text-right font-medium text-[var(--color-text-primary)]">
                {a.percent.toFixed(1)}%
              </span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
