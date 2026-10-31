import type { ClassifierSource, TicketPriority, TicketStatus } from '../api/types.ts'
import { label } from '../format.ts'

export function StatusBadge({ status }: { status: TicketStatus }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{label(status)}</span>
}

export function PriorityBadge({ priority }: { priority: TicketPriority }) {
  return <span className={`badge priority-${priority.toLowerCase()}`}>{label(priority)}</span>
}

export function ClassifierBadge({ source }: { source: ClassifierSource }) {
  const isAi = source === 'GEMINI'
  return (
    <span className={`badge ${isAi ? 'source-ai' : 'source-rules'}`} title={isAi ? 'Sorted by Gemini' : 'Sorted by keyword rules'}>
      {isAi ? 'AI' : 'Rules'}
    </span>
  )
}
