'use client'

import { useState } from 'react'
import { useTimeMachine } from '@/hooks/useTimeMachine'
import TimeMachineDateSelector from '@/components/timeMachine/TimeMachineDateSelector'
import TimeMachineSummaryHeader from '@/components/timeMachine/TimeMachineSummaryHeader'
import TimeMachineHoldingsTable from '@/components/timeMachine/TimeMachineHoldingsTable'
import TimeMachineDecisionsList from '@/components/timeMachine/TimeMachineDecisionsList'
import {
  TimeMachineEmpty,
  TimeMachineError,
  TimeMachineSkeleton,
} from '@/components/timeMachine/TimeMachineStates'

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10)
}

export default function TimeMachinePage() {
  const [asOfDate, setAsOfDate] = useState(todayIsoDate)
  const { data: result, isPending, isError, refetch } = useTimeMachine(asOfDate)

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">
          Portfolio Time Machine
        </h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Replay your trades to see your portfolio and best/worst decisions on a past date.
        </p>
      </div>

      <TimeMachineDateSelector value={asOfDate} onChange={setAsOfDate} />

      {isPending && <TimeMachineSkeleton />}

      {isError && <TimeMachineError onRetry={() => refetch()} />}

      {!isPending && !isError && result && (
        <>
          <TimeMachineSummaryHeader result={result} />

          {result.holdings.length === 0 ? (
            <TimeMachineEmpty />
          ) : (
            <TimeMachineHoldingsTable holdings={result.holdings} />
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <TimeMachineDecisionsList title="Best Decisions" decisions={result.bestDecisions} />
            <TimeMachineDecisionsList title="Worst Decisions" decisions={result.worstDecisions} />
          </div>
        </>
      )}
    </div>
  )
}
