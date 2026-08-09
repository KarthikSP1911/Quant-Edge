'use client'

const WINDOW_DAYS = 730

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10)
}

function daysBetween(from: string, to: string): number {
  return Math.round((new Date(to).getTime() - new Date(from).getTime()) / (1000 * 60 * 60 * 24))
}

function addDays(isoDate: string, days: number): string {
  const date = new Date(isoDate)
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

export default function TimeMachineDateSelector({
  value,
  onChange,
}: {
  value: string
  onChange: (isoDate: string) => void
}) {
  const today = toIsoDate(new Date())
  const minDate = addDays(today, -WINDOW_DAYS)
  const sliderValue = Math.min(WINDOW_DAYS, Math.max(0, daysBetween(minDate, value)))

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4 sm:flex-row sm:items-center sm:gap-4">
      <label className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)]">
        As of
        <input
          type="date"
          value={value}
          min={minDate}
          max={today}
          onChange={(e) => onChange(e.target.value)}
          className="rounded-md border border-[var(--color-border)] bg-[var(--color-page-bg)] px-2 py-1 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:ring-1 focus:ring-[var(--color-accent-blue)] focus:outline-none"
        />
      </label>
      <input
        type="range"
        min={0}
        max={WINDOW_DAYS}
        value={sliderValue}
        onChange={(e) => onChange(addDays(minDate, Number(e.target.value)))}
        className="flex-1 accent-[var(--color-accent-blue)]"
        aria-label="As of date"
      />
    </div>
  )
}
