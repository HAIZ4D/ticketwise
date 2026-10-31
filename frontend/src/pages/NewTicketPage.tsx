import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { api } from '../api/endpoints.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'

export function NewTicketPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const create = useMutation({
    mutationFn: () => api.createTicket(title, description),
    onSuccess: (ticket) => {
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
      navigate(`/tickets/${ticket.id}`)
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    create.mutate()
  }

  return (
    <div className="narrow page-stack">
      <title>New ticket · TicketWise</title>
      <div>
        <h1>Report a problem</h1>
        <p className="muted">Describe it in your own words. It will be sorted by category and priority for you.</p>
      </div>
      <form onSubmit={handleSubmit}>
        <label>
          Title
          <input
            required
            minLength={5}
            maxLength={120}
            aria-describedby="title-help"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
          <small id="title-help">A short summary, 5 to 120 characters.</small>
        </label>
        <label>
          Description
          <textarea
            required
            minLength={10}
            maxLength={2000}
            aria-describedby="description-help"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <small id="description-help">What happened, and since when? 10 to 2000 characters.</small>
        </label>
        <ErrorMessage error={create.error} />
        <div>
          <button type="submit" disabled={create.isPending}>
            {create.isPending ? 'Sorting your ticket…' : 'Submit ticket'}
          </button>
        </div>
      </form>
    </div>
  )
}
