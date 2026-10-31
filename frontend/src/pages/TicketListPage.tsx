import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api/endpoints.ts'
import type { TicketFilters } from '../api/endpoints.ts'
import { CATEGORIES, PRIORITIES, STATUSES } from '../api/types.ts'
import { isStaff, useCurrentUser } from '../auth/useAuth.ts'
import { ClassifierBadge, PriorityBadge, StatusBadge } from '../components/Badges.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { count, formatDate, label } from '../format.ts'

const SORTS = [
  { value: 'createdAt,desc', name: 'Newest first' },
  { value: 'createdAt,asc', name: 'Oldest first' },
  { value: 'updatedAt,desc', name: 'Recently updated' },
]

export function TicketListPage() {
  const user = useCurrentUser()
  const staff = isStaff(user)
  const [params, setParams] = useSearchParams()
  const page = Number.parseInt(params.get('page') ?? '', 10) || 1
  const mine = staff && params.get('mine') === '1'

  const filters: TicketFilters = {
    status: params.get('status') ?? undefined,
    priority: params.get('priority') ?? undefined,
    category: params.get('category') ?? undefined,
    assigneeId: mine ? user.id : undefined,
    sort: params.get('sort') ?? SORTS[0].value,
    page: page - 1,
  }
  const tickets = useQuery({
    queryKey: ['tickets', filters],
    queryFn: () => api.listTickets(filters),
    placeholderData: keepPreviousData,
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
          New ticket
        </Link>
      </div>

      <section className="filters" aria-label="Filters">
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
          <label className="checkbox">
            <input type="checkbox" checked={mine} onChange={(e) => setFilter('mine', e.target.checked ? '1' : '')} />
            My queue
          </label>
        )}
      </section>

      <ErrorMessage error={tickets.error} />
      {tickets.isPending && <p className="status-text">Loading tickets…</p>}

      {tickets.data && tickets.data.content.length === 0 && (
        <p className="status-text card">No tickets match these filters.</p>
      )}

      {tickets.data && tickets.data.content.length > 0 && (
        <>
          <div className="card table-card">
            <table>
              <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">Title</th>
                  <th scope="col">Status</th>
                  <th scope="col">Priority</th>
                  <th scope="col" className="wide-only">
                    Category
                  </th>
                  {staff && (
                    <th scope="col" className="wide-only">
                      Requester
                    </th>
                  )}
                  {staff && (
                    <th scope="col" className="wide-only">
                      Assignee
                    </th>
                  )}
                  <th scope="col" className="wide-only">
                    Created
                  </th>
                </tr>
              </thead>
              <tbody>
                {tickets.data.content.map((ticket) => (
                  <tr key={ticket.id}>
                    <td className="muted">{ticket.id}</td>
                    <td className="title-cell">
                      <Link to={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                    </td>
                    <td>
                      <StatusBadge status={ticket.status} />
                    </td>
                    <td>
                      <PriorityBadge priority={ticket.priority} />
                    </td>
                    <td className="wide-only">
                      {label(ticket.category)} <ClassifierBadge source={ticket.classifiedBy} />
                    </td>
                    {staff && <td className="wide-only">{ticket.requesterEmail}</td>}
                    {staff && <td className="wide-only">{ticket.assigneeEmail ?? <span className="muted">None</span>}</td>}
                    <td className="wide-only nowrap">{formatDate(ticket.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <nav className="pager" aria-label="Pages">
            <button type="button" className="button-quiet" disabled={page <= 1} onClick={() => goToPage(page - 1)}>
              Previous
            </button>
            <span className="muted">
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
