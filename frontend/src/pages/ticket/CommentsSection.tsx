import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../../api/endpoints.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { formatDate } from '../../format.ts'

export function CommentsSection({ ticketId }: { ticketId: string }) {
  const queryClient = useQueryClient()
  const [body, setBody] = useState('')
  const comments = useQuery({ queryKey: ['comments', ticketId], queryFn: () => api.listComments(ticketId) })
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

  return (
    <section className="card">
      <h2>Comments</h2>
      <ErrorMessage error={comments.error} />
      {comments.data?.length === 0 && <p className="muted">No comments yet.</p>}
      <ul className="thread">
        {comments.data?.map((comment) => (
          <li key={comment.id}>
            <p className="meta">
              <strong>{comment.authorEmail}</strong> · {formatDate(comment.createdAt)}
            </p>
            <p className="description">{comment.body}</p>
          </li>
        ))}
      </ul>
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
