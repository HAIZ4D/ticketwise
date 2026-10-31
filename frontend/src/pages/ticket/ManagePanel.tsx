import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../../api/endpoints.ts'
import type { Classification } from '../../api/endpoints.ts'
import { CATEGORIES, PRIORITIES } from '../../api/types.ts'
import type { Ticket, TicketCategory, TicketPriority } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { label } from '../../format.ts'

export function ManagePanel({ ticket }: { ticket: Ticket }) {
  const queryClient = useQueryClient()
  const agents = useQuery({ queryKey: ['agents'], queryFn: api.listAgents })
  const action = useMutation({
    mutationFn: (run: () => Promise<Ticket>) => run(),
    onSuccess: (updated) => {
      queryClient.setQueryData(['ticket', String(ticket.id)], updated)
      queryClient.invalidateQueries({ queryKey: ['history', String(ticket.id)] })
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
    },
  })
  const assigneeMissing = ticket.assigneeId !== null && !agents.data?.some((agent) => agent.id === ticket.assigneeId)

  function assign(value: string) {
    action.mutate(() => api.assign(ticket.id, value ? Number(value) : null))
  }

  return (
    <section className="card manage" aria-label="Manage ticket">
      <h2>Manage</h2>
      <ErrorMessage error={action.error ?? agents.error} />

      <div>
        <h3>Move to</h3>
        {ticket.nextStatuses.length === 0 && <p className="muted">This ticket is closed.</p>}
        <div className="button-row">
          {ticket.nextStatuses.map((status) => (
            <button
              key={status}
              type="button"
              className="button-quiet"
              disabled={action.isPending}
              onClick={() => action.mutate(() => api.changeStatus(ticket.id, status))}
            >
              {label(status)}
            </button>
          ))}
        </div>
      </div>

      <label>
        Assignee
        <select value={ticket.assigneeId ?? ''} disabled={action.isPending} onChange={(e) => assign(e.target.value)}>
          <option value="">None</option>
          {agents.data?.map((agent) => (
            <option key={agent.id} value={agent.id}>
              {agent.email}
            </option>
          ))}
          {assigneeMissing && <option value={ticket.assigneeId ?? ''}>{ticket.assigneeEmail}</option>}
        </select>
      </label>

      <OverrideForm
        key={`${ticket.category}-${ticket.priority}`}
        ticket={ticket}
        disabled={action.isPending}
        onSave={(classification) => action.mutate(() => api.overrideClassification(ticket.id, classification))}
      />

      <button
        type="button"
        className="button-quiet"
        disabled={action.isPending}
        onClick={() => action.mutate(() => api.reclassify(ticket.id))}
      >
        Run classifier again
      </button>
    </section>
  )
}

interface OverrideFormProps {
  ticket: Ticket
  disabled: boolean
  onSave: (classification: Classification) => void
}

function OverrideForm({ ticket, disabled, onSave }: OverrideFormProps) {
  const [category, setCategory] = useState<TicketCategory>(ticket.category)
  const [priority, setPriority] = useState<TicketPriority>(ticket.priority)
  const changed = category !== ticket.category || priority !== ticket.priority

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSave({
      category: category !== ticket.category ? category : undefined,
      priority: priority !== ticket.priority ? priority : undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit}>
      <label>
        Category
        <select value={category} onChange={(e) => setCategory(e.target.value as TicketCategory)}>
          {CATEGORIES.map((option) => (
            <option key={option} value={option}>
              {label(option)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Priority
        <select value={priority} onChange={(e) => setPriority(e.target.value as TicketPriority)}>
          {PRIORITIES.map((option) => (
            <option key={option} value={option}>
              {label(option)}
            </option>
          ))}
        </select>
      </label>
      <div>
        <button type="submit" disabled={disabled || !changed}>
          Save override
        </button>
      </div>
    </form>
  )
}
