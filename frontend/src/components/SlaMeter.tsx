import type { SlaState } from '../api/types.ts'
import { timeLeft } from '../format.ts'
import { SlaBadge } from './Badges.tsx'

interface SlaMeterProps {
  state: SlaState
  createdAt: string
  dueAt: string
  now: number
}

const RUNNING_STATES: SlaState[] = ['ON_TRACK', 'DUE_SOON', 'OVERDUE']

export function SlaMeter({ state, createdAt, dueAt, now }: SlaMeterProps) {
  if (!RUNNING_STATES.includes(state)) {
    return <SlaBadge state={state} />
  }
  const start = new Date(createdAt).getTime()
  const end = new Date(dueAt).getTime()
  const used = Math.min(Math.max((now - start) / (end - start), 0), 1)

  return (
    <div className={`sla sla-state-${state.toLowerCase()}`}>
      <div className="sla-text">
        <SlaBadge state={state} />
        <span className="mono">{timeLeft(dueAt, now)}</span>
      </div>
      <div className="sla-track">
        <div className="sla-fill" style={{ width: `${used * 100}%` }} />
      </div>
    </div>
  )
}
