import type { ClassifierSource, SlaState, TicketPriority, TicketStatus } from '../api/types.ts'
import { initials, label } from '../format.ts'
import { PriorityIcon, StatusIcon } from './Icons.tsx'

export function StatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`badge status-${status.toLowerCase()}`}>
      <StatusIcon status={status} />
      <span className="label-wide">{label(status)}</span>
    </span>
  )
}

export function PriorityBadge({ priority }: { priority: TicketPriority }) {
  return (
    <span className={`badge priority-${priority.toLowerCase()}`}>
      <PriorityIcon priority={priority} />
      <span className="label-wide">{label(priority)}</span>
    </span>
  )
}

export function SlaBadge({ state }: { state: SlaState }) {
  if (state === 'NONE') {
    return null
  }
  return <span className={`badge sla-${state.toLowerCase()}`}>{label(state)}</span>
}

export function ClassifierBadge({ source }: { source: ClassifierSource }) {
  const isAi = source === 'GEMINI'
  return (
    <span className={`tag ${isAi ? 'source-ai' : 'source-rules'}`} title={isAi ? 'Sorted by Gemini' : 'Sorted by keyword rules'}>
      {isAi ? 'AI' : 'Rules'}
    </span>
  )
}

export function Avatar({ email }: { email: string }) {
  return (
    <span className="avatar" aria-hidden="true">
      {initials(email)}
    </span>
  )
}

export function Person({ email }: { email: string }) {
  return (
    <span className="person">
      <Avatar email={email} />
      <span>{email}</span>
    </span>
  )
}
