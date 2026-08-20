'use client'

import { useState, useEffect } from 'react'
import { triggerResearch } from '@/lib/api/research'
import { createSseConnection } from '@/lib/sse/client'

interface ResearchAgentBodyProps {
  symbol: string
}

interface TraceEvent {
  step: string
  message: string
}

// Visual treatment per phase the agent orchestrator emits (planning/plan, tool_call, observation,
// replan, final, saving_report, complete, error) - see ResearchAgentOrchestrator.TraceEvent.
const STEP_STYLES: Record<string, { label: string; dotClass: string }> = {
  planning: { label: 'Planning', dotClass: 'bg-[var(--color-accent-blue)]' },
  plan: { label: 'Plan', dotClass: 'bg-[var(--color-accent-blue)]' },
  tool_call: { label: 'Tool call', dotClass: 'bg-[var(--color-text-secondary)]' },
  observation: { label: 'Observation', dotClass: 'bg-[var(--color-profit)]' },
  replan: { label: 'Replanning', dotClass: 'bg-[var(--color-warning)]' },
  final: { label: 'Final report', dotClass: 'bg-[var(--color-profit)]' },
  saving_report: { label: 'Saving', dotClass: 'bg-[var(--color-accent-blue)]' },
}

function stepStyle(step: string) {
  return STEP_STYLES[step] ?? { label: step.replace(/_/g, ' '), dotClass: 'bg-[var(--color-accent-blue)]' }
}

export default function ResearchAgentBody({ symbol }: ResearchAgentBodyProps) {
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
      eventSource.close()
    }

    return () => {
      eventSource.close()
    }
  }, [sessionId])

  return (
    <div className="flex-1 overflow-y-auto">
      {status === 'error' && (
        <div className="mb-4 rounded-md bg-[var(--color-loss)]/10 p-4 text-[var(--color-loss)]">
          {error}
        </div>
      )}

      <div className="space-y-4">
        {traces.map((trace, i) => {
          const { label, dotClass } = stepStyle(trace.step)
          return (
            <div key={i} className="flex items-start gap-3">
              <div className="mt-1 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-[var(--color-accent-blue)]/20">
                <div className={`h-2 w-2 rounded-full ${dotClass}`} />
              </div>
              <div className="min-w-0">
                <span className="block text-xs font-semibold uppercase text-[var(--color-accent-blue)]">
                  {label}
                </span>
                {trace.step === 'final' ? (
                  <span className="block whitespace-pre-wrap text-sm text-[var(--color-text-primary)]">
                    {trace.message}
                  </span>
                ) : (
                  <span className="text-sm text-[var(--color-text-primary)]">{trace.message}</span>
                )}
              </div>
            </div>
          )
        })}

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
  )
}
