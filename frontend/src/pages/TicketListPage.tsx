import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api/endpoints.ts'
import type { TicketFilters } from '../api/endpoints.ts'
import { CATEGORIES, PRIORITIES, STATUSES } from '../api/types.ts'
import { isStaff, useCurrentUser } from '../auth/useAuth.ts'
import { ClassifierBadge, Person, PriorityBadge, StatusBadge } from '../components/Badges.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { SkeletonRows } from '../components/Skeleton.tsx'
import { SlaMeter } from '../components/SlaMeter.tsx'
import { useNow } from '../components/useNow.ts'
import { count, formatDate, label } from '../format.ts'

const REFRESH_MS = 60_000

const SORTS = [
  { value: 'createdAt,desc', name: 'Newest first' },
  { value: 'createdAt,asc', name: 'Oldest first' },
  { value: 'updatedAt,desc', name: 'Recently updated' },
]

export function TicketListPage() {
  const user = useCurrentUser()
  const staff = isStaff(user)
  const now = useNow()
  const [params, setParams] = useSearchParams()
  const page = Number.parseInt(params.get('page') ?? '', 10) || 1
  const mine = staff && params.get('mine') === '1'
  const overdue = staff && params.get('overdue') === '1'

  const filters: TicketFilters = {
    status: params.get('status') ?? undefined,
    priority: params.get('priority') ?? undefined,
    category: params.get('category') ?? undefined,
    assigneeId: mine ? user.id : undefined,
    overdue: overdue || undefined,
    sort: params.get('sort') ?? SORTS[0].value,
    page: page - 1,
  }
  const tickets = useQuery({
    queryKey: ['tickets', filters],
    queryFn: () => api.listTickets(filters),
    placeholderData: keepPreviousData,
    refetchInterval: REFRESH_MS,
  })

  function setFilter(name: string, value: string) {
    setParams((current) => {
      const next = new URLSearchParams(current)
      next.delete('page')
      if (value) {
        next.set(name, value)
      } else {
        next.delete(name)
      }
      return next
    })
  }

  function goToPage(newPage: number) {
    setParams((current) => {
      const next = new URLSearchParams(current)
      next.set('page', String(newPage))
      return next
    })
  }

  return (
    <>
      <title>Tickets · TicketWise</title>
      <div className="page-heading">
        <h1>{staff ? 'All tickets' : 'My tickets'}</h1>
        <Link className="button" to="/tickets/new">
          <Plus size={15} aria-hidden="true" />
          New ticket
        </Link>
      </div>

      <section className="toolbar" aria-label="Filters">
        <FilterSelect name="status" title="Status" options={STATUSES} value={filters.status} onChange={setFilter} />
        <FilterSelect
          name="priority"
          title="Priority"
          options={PRIORITIES}
          value={filters.priority}
          onChange={setFilter}
        />
        <FilterSelect
          name="category"
          title="Category"
          options={CATEGORIES}
          value={filters.category}
          onChange={setFilter}
        />
        <label>
          Sort
          <select value={filters.sort} onChange={(e) => setFilter('sort', e.target.value)}>
            {SORTS.map((sort) => (
              <option key={sort.value} value={sort.value}>
                {sort.name}
              </option>
            ))}
          </select>
        </label>
        {staff && (
          <label className="toggle">
            <input type="checkbox" checked={mine} onChange={(e) => setFilter('mine', e.target.checked ? '1' : '')} />
            My queue
          </label>
        )}
        {staff && (
          <label className="toggle">
            <input
              type="checkbox"
              checked={overdue}
              onChange={(e) => setFilter('overdue', e.target.checked ? '1' : '')}
            />
            Overdue only
          </label>
        )}
      </section>

      <ErrorMessage error={tickets.error} />
      {tickets.isPending && <SkeletonRows label="Loading tickets…" />}

      {tickets.data && tickets.data.content.length === 0 && (
        <div className="table-wrap empty">
          <strong>No tickets match these filters.</strong>
          <span>{staff ? 'Change a filter to see more.' : 'When you report a problem, it will show up here.'}</span>
        </div>
      )}

      {tickets.data && tickets.data.content.length > 0 && (
        <>
          <div className="table-wrap" aria-busy={tickets.isPlaceholderData}>
            <table className="queue">
              <thead>
                <tr>
                  <th scope="col" className="col-medium">
                    #
                  </th>
                  <th scope="col">Title</th>
                  <th scope="col">Status</th>
                  <th scope="col">Priority</th>
                  <th scope="col">SLA</th>
                  {staff && (
                    <th scope="col" className="col-wide">
                      Assignee
                    </th>
                  )}
                  <th scope="col" className="col-wide">
                    Created
                  </th>
                </tr>
              </thead>
              <tbody>
                {tickets.data.content.map((ticket) => (
                  <tr key={ticket.id}>
                    <td className="ticket-id mono col-medium">{ticket.id}</td>
                    <td className="title-cell">
                      <Link to={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                      <div className="title-meta">
                        <span>{label(ticket.category)}</span>
                        <ClassifierBadge source={ticket.classifiedBy} />
                        {staff && <span className="col-medium">{ticket.requesterEmail}</span>}
                      </div>
                    </td>
                    <td>
                      <StatusBadge status={ticket.status} />
                    </td>
                    <td>
                      <PriorityBadge priority={ticket.priority} />
                    </td>
                    <td>
                      <SlaMeter state={ticket.slaState} createdAt={ticket.createdAt} dueAt={ticket.slaDueAt} now={now} />
                    </td>
                    {staff && (
                      <td className="col-wide assignee">
                        {ticket.assigneeEmail ? <Person email={ticket.assigneeEmail} /> : <span className="muted">None</span>}
                      </td>
                    )}
                    <td className="col-wide mono muted">{formatDate(ticket.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <nav className="pager" aria-label="Pages">
            <button type="button" className="button-quiet" disabled={page <= 1} onClick={() => goToPage(page - 1)}>
              Previous
            </button>
            <span>
              Page {page} of {Math.max(tickets.data.totalPages, 1)} · {count(tickets.data.totalElements, 'ticket')}
            </span>
            <button
              type="button"
              className="button-quiet"
              disabled={page >= tickets.data.totalPages}
              onClick={() => goToPage(page + 1)}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </>
  )
}

interface FilterSelectProps {
  name: string
  title: string
  options: string[]
  value?: string
  onChange: (name: string, value: string) => void
}

function FilterSelect({ name, title, options, value, onChange }: FilterSelectProps) {
  return (
    <label>
      {title}
      <select value={value ?? ''} onChange={(e) => onChange(name, e.target.value)}>
        <option value="">All</option>
        {options.map((option) => (
          <option key={option} value={option}>
            {label(option)}
          </option>
        ))}
      </select>
    </label>
  )
}
