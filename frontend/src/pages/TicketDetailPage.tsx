import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { api } from '../api/endpoints.ts'
import { isStaff, useCurrentUser } from '../auth/useAuth.ts'
import { ClassifierBadge, PriorityBadge, StatusBadge } from '../components/Badges.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { SkeletonRows } from '../components/Skeleton.tsx'
import { SlaMeter } from '../components/SlaMeter.tsx'
import { useNow } from '../components/useNow.ts'
import { formatDate, label } from '../format.ts'
import { ActivitySection } from './ticket/ActivitySection.tsx'
import { DuplicatesPanel } from './ticket/DuplicatesPanel.tsx'
import { ManagePanel } from './ticket/ManagePanel.tsx'
import { SuggestionPanel } from './ticket/SuggestionPanel.tsx'

const REFRESH_MS = 60_000

export function TicketDetailPage() {
  const { id = '' } = useParams()
  const user = useCurrentUser()
  const now = useNow()
  const query = useQuery({
    queryKey: ['ticket', id],
    queryFn: () => api.getTicket(id),
    refetchInterval: REFRESH_MS,
  })

  if (query.isPending) {
    return <SkeletonRows label="Loading ticket…" rows={4} />
  }
  if (query.isError) {
    return (
      <>
        <ErrorMessage error={query.error} />
        <Link className="back-link" to="/tickets">
          Back to tickets
        </Link>
      </>
    )
  }

  const ticket = query.data
  return (
    <>
      <title>{`#${ticket.id} ${ticket.title} · TicketWise`}</title>
      <Link className="back-link" to="/tickets">
        <ArrowLeft size={14} aria-hidden="true" />
        Back to tickets
      </Link>
      <div className="detail" key={ticket.id}>
        <div className="detail-main">
          <header className="ticket-head">
            <p className="mono muted">Ticket #{ticket.id}</p>
            <h1>{ticket.title}</h1>
            <p className="badges">
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
              <span className="badge">{label(ticket.category)}</span>
            </p>
            <SlaMeter state={ticket.slaState} createdAt={ticket.createdAt} dueAt={ticket.slaDueAt} now={now} />
          </header>
          <p className="description ticket-body">{ticket.description}</p>
          {ticket.resolution && (
            <section className="resolution">
              <h2>Resolution</h2>
              <p className="description">{ticket.resolution}</p>
            </section>
          )}
          <ActivitySection ticketId={id} />
        </div>

        <aside className="inspector">
          {isStaff(user) && <ManagePanel ticket={ticket} />}
          {isStaff(user) && <DuplicatesPanel ticketId={id} />}
          {isStaff(user) && <SuggestionPanel ticketId={id} />}
          <section>
            <h2>
              Classification <ClassifierBadge source={ticket.classifiedBy} />
            </h2>
            <p>
              {label(ticket.aiCategory)}, {label(ticket.aiPriority)} priority
            </p>
            <p className="muted">{ticket.aiReason}</p>
            {ticket.overridden && <p className="note">An agent changed this suggestion.</p>}
          </section>
          <section>
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
        </aside>
      </div>
    </>
  )
}
