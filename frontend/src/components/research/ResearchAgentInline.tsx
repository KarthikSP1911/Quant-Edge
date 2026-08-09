'use client'

import { useState } from 'react'
import ResearchAgentBody from '@/components/research/ResearchAgentBody'

export default function ResearchAgentInline() {
  const [input, setInput] = useState('')
  const [activeSymbol, setActiveSymbol] = useState<string | null>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = input.trim().toUpperCase()
    if (!trimmed) return
    setActiveSymbol(trimmed)
  }

  return (
    <div className="flex flex-col gap-4">
      <form onSubmit={handleSubmit} className="flex items-center gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Enter a ticker symbol, e.g. AAPL"
          className="w-64 rounded-md border border-[var(--color-border)] bg-white px-4 py-2 text-sm text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-accent-blue)] focus:outline-none"
        />
        <button
          type="submit"
          disabled={!input.trim()}
          className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        >
          Research
        </button>
      </form>

      {!activeSymbol && (
        <div className="flex flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-[var(--color-border)] py-16 text-center">
          <p className="text-sm font-medium text-[var(--color-text-primary)]">No research yet</p>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Enter a ticker above to kick off an autonomous research run.
          </p>
        </div>
      )}

      {activeSymbol && (
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4">
          <h2 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            Autonomous research: {activeSymbol}
          </h2>
          <ResearchAgentBody key={activeSymbol} symbol={activeSymbol} />
        </div>
      )}
    </div>
  )
}
