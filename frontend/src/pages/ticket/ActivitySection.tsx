import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../../api/endpoints.ts'
import type { Comment, HistoryEntry } from '../../api/types.ts'
import { Avatar } from '../../components/Badges.tsx'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { formatDate, label } from '../../format.ts'

type ActivityItem = { at: string; key: string; comment: Comment } | { at: string; key: string; change: HistoryEntry }

export function ActivitySection({ ticketId }: { ticketId: string }) {
  const queryClient = useQueryClient()
  const [body, setBody] = useState('')
  const comments = useQuery({ queryKey: ['comments', ticketId], queryFn: () => api.listComments(ticketId) })
  const history = useQuery({ queryKey: ['history', ticketId], queryFn: () => api.listHistory(ticketId) })
  const add = useMutation({
    mutationFn: () => api.addComment(ticketId, body),
    onSuccess: () => {
      setBody('')
      queryClient.invalidateQueries({ queryKey: ['comments', ticketId] })
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    add.mutate()
  }

  const items = timeline(comments.data ?? [], history.data ?? [])
  const loaded = comments.data !== undefined && history.data !== undefined

  return (
    <section className="activity">
      <h2>Activity</h2>
      <ErrorMessage error={comments.error ?? history.error} />
      {loaded && items.length === 0 && <p className="muted">No activity yet.</p>}
      <ol className="thread">
        {items.map((item) =>
          'comment' in item ? (
            <li key={item.key}>
              <Avatar email={item.comment.authorEmail} />
              <div>
                <p className="meta">
                  <strong>{item.comment.authorEmail}</strong> · {formatDate(item.comment.createdAt)}
                </p>
                <p className="description">{item.comment.body}</p>
              </div>
            </li>
          ) : (
            <li key={item.key}>
              <span className="thread-dot" aria-hidden="true" />
              <div>
                <p className="change">
                  {label(item.change.field)}: {display(item.change, item.change.oldValue)} →{' '}
                  {display(item.change, item.change.newValue)}
                </p>
                <p className="meta">
                  {item.change.changedBy} · {formatDate(item.change.changedAt)}
                </p>
              </div>
            </li>
          ),
        )}
      </ol>
      <form onSubmit={handleSubmit}>
        <label>
          Add a comment
          <textarea required maxLength={1000} value={body} onChange={(e) => setBody(e.target.value)} />
        </label>
        <ErrorMessage error={add.error} />
        <div>
          <button type="submit" disabled={add.isPending}>
            {add.isPending ? 'Adding…' : 'Add comment'}
          </button>
        </div>
      </form>
    </section>
  )
}

function timeline(comments: Comment[], history: HistoryEntry[]): ActivityItem[] {
  const items: ActivityItem[] = [
    ...comments.map((comment) => ({ at: comment.createdAt, key: `comment-${comment.id}`, comment })),
    ...history.map((change) => ({ at: change.changedAt, key: `change-${change.id}`, change })),
  ]
  return items.sort((first, second) => first.at.localeCompare(second.at))
}

function display(entry: HistoryEntry, value: string | null): string {
  if (value === null) {
    return 'None'
  }
  return entry.field === 'assignee' ? value : label(value)
}
