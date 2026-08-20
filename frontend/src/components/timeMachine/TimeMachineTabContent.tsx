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

export default function TimeMachineTabContent() {
  const [asOfDate, setAsOfDate] = useState(todayIsoDate)
  const { data: result, isPending, isError, refetch } = useTimeMachine(asOfDate)

  return (
    <div className="flex flex-col gap-6">
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
