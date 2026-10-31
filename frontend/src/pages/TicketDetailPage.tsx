import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { api } from '../api/endpoints.ts'
import { isStaff, useCurrentUser } from '../auth/useAuth.ts'
import { ClassifierBadge, PriorityBadge, SlaBadge, StatusBadge } from '../components/Badges.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { formatDate, label } from '../format.ts'
import { CommentsSection } from './ticket/CommentsSection.tsx'
import { DuplicatesPanel } from './ticket/DuplicatesPanel.tsx'
import { HistorySection } from './ticket/HistorySection.tsx'
import { ManagePanel } from './ticket/ManagePanel.tsx'
import { SuggestionPanel } from './ticket/SuggestionPanel.tsx'

export function TicketDetailPage() {
  const { id = '' } = useParams()
  const user = useCurrentUser()
  const query = useQuery({ queryKey: ['ticket', id], queryFn: () => api.getTicket(id) })

  if (query.isPending) {
    return <p className="status-text">Loading ticket…</p>
  }
  if (query.isError) {
    return (
      <>
        <ErrorMessage error={query.error} />
        <Link to="/tickets">Back to tickets</Link>
      </>
    )
  }

  const ticket = query.data
  return (
    <>
      <title>{`#${ticket.id} ${ticket.title} · TicketWise`}</title>
      <Link to="/tickets">Back to tickets</Link>
      <div className="detail" key={ticket.id}>
        <div className="detail-main">
          <section className="card">
            <p className="muted">Ticket #{ticket.id}</p>
            <h1>{ticket.title}</h1>
            <p className="badges">
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
              <span className="badge">{label(ticket.category)}</span>
              <SlaBadge state={ticket.slaState} />
            </p>
            <p className="description">{ticket.description}</p>
          </section>
          {ticket.resolution && (
            <section className="card">
              <h2>Resolution</h2>
              <p className="description">{ticket.resolution}</p>
            </section>
          )}
          <CommentsSection ticketId={id} />
          <HistorySection ticketId={id} />
        </div>

        <aside className="detail-side">
          <section className="card">
            <h2>Details</h2>
            <dl>
              <dt>Requester</dt>
              <dd>{ticket.requesterEmail}</dd>
              <dt>Assignee</dt>
              <dd>{ticket.assigneeEmail ?? 'None'}</dd>
              <dt>Target</dt>
              <dd>{formatDate(ticket.slaDueAt)}</dd>
              <dt>Created</dt>
              <dd>{formatDate(ticket.createdAt)}</dd>
              <dt>Updated</dt>
              <dd>{formatDate(ticket.updatedAt)}</dd>
              {ticket.resolvedAt && (
                <>
                  <dt>Resolved</dt>
                  <dd>{formatDate(ticket.resolvedAt)}</dd>
                </>
              )}
            </dl>
          </section>

          <section className="card">
            <h2>
              Classification <ClassifierBadge source={ticket.classifiedBy} />
            </h2>
            <p>
              {label(ticket.aiCategory)}, {label(ticket.aiPriority)} priority
            </p>
            <p className="muted">{ticket.aiReason}</p>
            {ticket.overridden && <p className="note">An agent changed this suggestion.</p>}
          </section>

          {isStaff(user) && (
            <>
              <DuplicatesPanel ticketId={id} />
              <SuggestionPanel ticketId={id} />
              <ManagePanel ticket={ticket} />
            </>
          )}
        </aside>
      </div>
    </>
  )
}
