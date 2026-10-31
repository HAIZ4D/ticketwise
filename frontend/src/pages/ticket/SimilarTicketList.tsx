import { Link } from 'react-router'
import type { SimilarTicket } from '../../api/types.ts'
import { StatusBadge } from '../../components/Badges.tsx'

export function SimilarTicketList({ tickets }: { tickets: SimilarTicket[] }) {
  return (
    <ul className="similar">
      {tickets.map((ticket) => (
        <li key={ticket.id}>
          <Link to={`/tickets/${ticket.id}`}>
            #{ticket.id} {ticket.title}
          </Link>
          <span className="meta">
            <StatusBadge status={ticket.status} /> {Math.round(ticket.score * 100)}% similar
          </span>
        </li>
      ))}
    </ul>
  )
}
