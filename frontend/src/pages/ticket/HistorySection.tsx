import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/endpoints.ts'
import type { HistoryEntry } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { formatDate, label } from '../../format.ts'

export function HistorySection({ ticketId }: { ticketId: string }) {
  const history = useQuery({ queryKey: ['history', ticketId], queryFn: () => api.listHistory(ticketId) })

  return (
    <section className="card">
      <h2>History</h2>
      <ErrorMessage error={history.error} />
      {history.data?.length === 0 && <p className="muted">No changes yet.</p>}
      <ul className="thread">
        {history.data?.map((entry) => (
          <li key={entry.id}>
            <p>
              {label(entry.field)}: {display(entry, entry.oldValue)} → {display(entry, entry.newValue)}
            </p>
            <p className="meta">
              {entry.changedBy} · {formatDate(entry.changedAt)}
            </p>
          </li>
        ))}
      </ul>
    </section>
  )
}

function display(entry: HistoryEntry, value: string | null): string {
  if (value === null) {
    return 'None'
  }
  return entry.field === 'assignee' ? value : label(value)
}
