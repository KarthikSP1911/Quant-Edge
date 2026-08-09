'use client'

import ResearchAgentBody from '@/components/research/ResearchAgentBody'

interface ResearchAgentPanelProps {
  symbol: string
  onClose: () => void
}

export default function ResearchAgentPanel({ symbol, onClose }: ResearchAgentPanelProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-xl border border-[var(--color-border)] bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-[var(--color-border)] p-4">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
            Autonomous Research: {symbol}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="flex h-8 w-8 items-center justify-center rounded-md text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-sidebar-hover)] hover:text-[var(--color-text-primary)]"
          >
            ✕
          </button>
        </div>

        <div className="flex flex-1 flex-col overflow-hidden p-4">
          <ResearchAgentBody symbol={symbol} />
        </div>
      </div>
    </div>
  )
}
