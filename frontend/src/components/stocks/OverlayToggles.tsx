'use client'

export interface OverlayState {
  bollinger: boolean
  rsi: boolean
  macd: boolean
}

const LABELS: { key: keyof OverlayState; label: string }[] = [
  { key: 'bollinger', label: 'Bollinger Bands' },
  { key: 'rsi', label: 'RSI' },
  { key: 'macd', label: 'MACD' },
]

export default function OverlayToggles({
  value,
  onChange,
}: {
  value: OverlayState
  onChange: (value: OverlayState) => void
}) {
  return (
    <div className="flex flex-wrap gap-4">
      {LABELS.map(({ key, label }) => (
        <label
          key={key}
          className="flex cursor-pointer items-center gap-2 text-sm text-[var(--color-text-secondary)]"
        >
          <input
            type="checkbox"
            checked={value[key]}
            onChange={(e) => onChange({ ...value, [key]: e.target.checked })}
            className="h-3.5 w-3.5 rounded border-[var(--color-border)] text-[var(--color-accent-blue)] focus:ring-[var(--color-accent-blue)]"
          />
          {label}
        </label>
      ))}
    </div>
  )
}
