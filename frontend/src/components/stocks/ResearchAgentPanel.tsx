'use client'

import { useState, useEffect } from 'react'
import { triggerResearch } from '@/lib/api/research'
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import ReactMarkdown from 'react-markdown'
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import remarkGfm from 'remark-gfm'
import { createSseConnection } from '@/lib/sse/client'

interface ResearchAgentPanelProps {
  symbol: string
  onClose: () => void
}

interface TraceEvent {
  step: string
  message: string
}

export default function ResearchAgentPanel({ symbol, onClose }: ResearchAgentPanelProps) {
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [traces, setTraces] = useState<TraceEvent[]>([])
  const [status, setStatus] = useState<'idle' | 'running' | 'completed' | 'error'>('idle')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const startResearch = async () => {
      try {
        setStatus('running')
        const { sessionId: newSessionId } = await triggerResearch(symbol)
        setSessionId(newSessionId)
      } catch (err) {
        setStatus('error')
        setError(err instanceof Error ? err.message : 'Failed to start research')
      }
    }

    void startResearch()
  }, [symbol])

  useEffect(() => {
    if (!sessionId) return

    const eventSource = createSseConnection(`/api/v1/agent/trace/${sessionId}`)

    eventSource.addEventListener('trace', (e) => {
      try {
        const data = JSON.parse(e.data) as TraceEvent
        setTraces((prev) => [...prev, data])

        if (data.step === 'complete') {
          setStatus('completed')
          eventSource.close()
        } else if (data.step === 'error') {
          setStatus('error')
          setError(data.message)
          eventSource.close()
        }
      } catch (err) {
        console.error('Failed to parse SSE event', err)
      }
    })

    eventSource.onerror = () => {
      // It might be complete if the server closes the connection gracefully,
      // but if we didn't receive 'complete', we log an error.
      eventSource.close()
    }

    return () => {
      eventSource.close()
    }
  }, [sessionId])

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

        <div className="flex-1 overflow-y-auto p-4">
          {status === 'error' && (
            <div className="mb-4 rounded-md bg-[var(--color-loss)]/10 p-4 text-[var(--color-loss)]">
              {error}
            </div>
          )}

          <div className="space-y-4">
            {traces.map((trace, i) => (
              <div key={i} className="flex items-start gap-3">
                <div className="mt-1 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-[var(--color-accent-blue)]/20">
                  <div className="h-2 w-2 rounded-full bg-[var(--color-accent-blue)]" />
                </div>
                <div>
                  <span className="block text-xs font-semibold uppercase text-[var(--color-accent-blue)]">
                    {trace.step.replace('_', ' ')}
                  </span>
                  <span className="text-sm text-[var(--color-text-primary)]">{trace.message}</span>
                </div>
              </div>
            ))}

            {status === 'running' && (
              <div className="flex items-center gap-3">
                <div className="mt-1 flex h-4 w-4 shrink-0 animate-pulse items-center justify-center rounded-full bg-gray-500/20">
                  <div className="h-2 w-2 rounded-full bg-gray-500" />
                </div>
                <div className="flex items-center gap-2">
                  <div
                    className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-text-secondary)]"
                    style={{ animationDelay: '0ms' }}
                  />
                  <div
                    className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-text-secondary)]"
                    style={{ animationDelay: '150ms' }}
                  />
                  <div
                    className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-text-secondary)]"
                    style={{ animationDelay: '300ms' }}
                  />
                </div>
              </div>
            )}

            {status === 'completed' && (
              <div className="mt-6 rounded-lg bg-[var(--color-profit)]/10 p-4 text-center text-sm font-medium text-[var(--color-profit)]">
                Research report successfully generated and saved to your notes!
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
