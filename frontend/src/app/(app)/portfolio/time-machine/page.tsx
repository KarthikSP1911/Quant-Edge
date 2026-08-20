'use client'

import TimeMachineTabContent from '@/components/timeMachine/TimeMachineTabContent'

export default function TimeMachinePage() {
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

      <TimeMachineTabContent />
    </div>
  )
}
