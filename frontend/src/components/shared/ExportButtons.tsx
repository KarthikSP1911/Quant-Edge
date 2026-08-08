'use client'

import { useState } from 'react'
import { downloadExport, type ExportKind } from '@/lib/api/exports'

const EXPORTS: { kind: ExportKind; label: string }[] = [
  { kind: 'portfolio-pdf', label: 'Portfolio PDF' },
  { kind: 'trade-history-csv', label: 'Trade History CSV' },
  { kind: 'tax-pnl-pdf', label: 'Tax P&L PDF' },
]

export default function ExportButtons() {
  const [pending, setPending] = useState<ExportKind | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleExport(kind: ExportKind) {
    setPending(kind)
    setError(null)
    try {
      await downloadExport(kind)
    } catch {
      setError('Failed to generate export. Please try again.')
    } finally {
      setPending(null)
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap gap-2">
        {EXPORTS.map(({ kind, label }) => (
          <button
            key={kind}
            type="button"
            onClick={() => void handleExport(kind)}
            disabled={pending !== null}
            className="rounded-md border border-[var(--color-border)] bg-[var(--color-card-bg)] px-3 py-1.5 text-sm font-medium text-[var(--color-text-primary)] hover:bg-[var(--color-sidebar-hover)] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {pending === kind ? 'Generating…' : label}
          </button>
        ))}
      </div>
      {error && <p className="text-xs text-[var(--color-loss)]">{error}</p>}
    </div>
  )
}
