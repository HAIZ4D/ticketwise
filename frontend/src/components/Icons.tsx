import type { TicketPriority, TicketStatus } from '../api/types.ts'

const PRIORITY_LEVELS: Record<TicketPriority, number> = { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 }

export function StatusIcon({ status }: { status: TicketStatus }) {
  return (
    <svg className="status-icon" width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <circle cx="7" cy="7" r="5.75" stroke="currentColor" strokeWidth="1.5" />
      {status === 'IN_PROGRESS' && <path d="M7 3.5a3.5 3.5 0 0 1 0 7z" fill="currentColor" />}
      {status === 'RESOLVED' && (
        <path d="m4.4 7.2 1.9 1.9 3.3-3.8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      )}
      {status === 'CLOSED' && <circle cx="7" cy="7" r="3.5" fill="currentColor" />}
    </svg>
  )
}

export function PriorityIcon({ priority }: { priority: TicketPriority }) {
  const level = PRIORITY_LEVELS[priority]
  return (
    <svg className="priority-icon" width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
      {[1, 2, 3, 4].map((bar) => (
        <rect
          key={bar}
          x={(bar - 1) * 3.5}
          y={12 - bar * 2.5}
          width="2.5"
          height={bar * 2.5 + 1}
          rx="0.5"
          fill="currentColor"
          opacity={bar <= level ? 1 : 0.22}
        />
      ))}
    </svg>
  )
}

export function LogoMark() {
  return (
    <svg width="20" height="20" viewBox="0 0 32 32" aria-hidden="true">
      <path
        fill="#c8f135"
        d="M3 8a3 3 0 0 1 3-3h20a3 3 0 0 1 3 3v4.5a3.5 3.5 0 0 0 0 7V24a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3v-4.5a3.5 3.5 0 0 0 0-7z"
      />
      <path fill="#15171a" d="M10.5 11h11v3h-4v8h-3v-8h-4z" />
    </svg>
  )
}
