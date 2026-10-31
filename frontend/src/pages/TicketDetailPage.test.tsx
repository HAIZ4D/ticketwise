import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Ticket, User } from '../api/types.ts'
import App from '../App.tsx'
import { Failure, aTicket, admin, agent, employee, logInAs, renderPage } from '../test/utils.tsx'

function openTicket(user: User, ticket: Ticket = aTicket(), routes: Record<string, unknown> = {}) {
  const api = logInAs(user, {
    'GET /tickets/7': ticket,
    'GET /tickets/7/comments': [],
    'GET /tickets/7/history': [],
    'GET /users/agents': [agent, admin],
    ...routes,
  })
  renderPage(<App />, '/tickets/7')
  return api
}

async function managePanel() {
  return within(await screen.findByRole('region', { name: 'Manage ticket' }))
}

describe('ticket detail', () => {
  it('shows the ticket and the AI suggestion', async () => {
    openTicket(employee)

    expect(await screen.findByRole('heading', { name: 'VPN keeps disconnecting' })).toBeInTheDocument()
    expect(screen.getByText('My VPN drops every 10 minutes')).toBeInTheDocument()
    expect(screen.getByText('AI')).toBeInTheDocument()
    expect(screen.getByText('Repeated VPN drops block work.')).toBeInTheDocument()
  })

  it('shows a Rules badge when the keyword rules sorted the ticket', async () => {
    openTicket(employee, aTicket({ classifiedBy: 'RULES' }))

    expect(await screen.findByText('Rules')).toBeInTheDocument()
    expect(screen.queryByText('AI')).not.toBeInTheDocument()
  })

  it('says so when an agent changed the suggestion', async () => {
    openTicket(employee, aTicket({ overridden: true, category: 'HARDWARE' }))

    expect(await screen.findByText('An agent changed this suggestion.')).toBeInTheDocument()
  })

  it('shows ticket text as plain text, never as HTML', async () => {
    const attack = '<img src=x onerror=alert(1)>'
    openTicket(employee, aTicket({ title: attack, description: attack }))

    expect(await screen.findByRole('heading', { name: attack })).toBeInTheDocument()
    expect(document.querySelector('img')).toBeNull()
  })

  it('shows the 404 message for a ticket the user cannot see', async () => {
    openTicket(employee, aTicket(), { 'GET /tickets/7': new Failure(404, 'Ticket 7 not found') })

    expect(await screen.findByRole('alert')).toHaveTextContent('Ticket 7 not found')
    expect(screen.getByRole('link', { name: 'Back to tickets' })).toBeInTheDocument()
  })

  it('hides staff controls from employees and never asks for the agent list', async () => {
    const api = openTicket(employee)

    await screen.findByRole('heading', { name: 'VPN keeps disconnecting' })
    expect(screen.queryByRole('region', { name: 'Manage ticket' })).not.toBeInTheDocument()
    expect(api.callsTo('GET /users/agents')).toHaveLength(0)
  })

  it.each([
    ['agent', agent],
    ['admin', admin],
  ])('shows staff controls to an %s', async (_name, user) => {
    openTicket(user)

    expect(await screen.findByRole('region', { name: 'Manage ticket' })).toBeInTheDocument()
  })
})

describe('managing a ticket', () => {
  it('offers only the moves the server allows', async () => {
    openTicket(agent, aTicket({ nextStatuses: ['RESOLVED'] }))
    const panel = await managePanel()

    expect(panel.getByRole('button', { name: 'Resolved' })).toBeInTheDocument()
    expect(panel.queryByRole('button', { name: 'Closed' })).not.toBeInTheDocument()
    expect(panel.queryByRole('button', { name: 'Open' })).not.toBeInTheDocument()
  })

  it('says a closed ticket has no moves left', async () => {
    openTicket(agent, aTicket({ status: 'CLOSED', nextStatuses: [] }))
    const panel = await managePanel()

    expect(panel.getByText('This ticket is closed.')).toBeInTheDocument()
  })

  it('changes status and shows the result', async () => {
    const updated = aTicket({ status: 'IN_PROGRESS', nextStatuses: ['RESOLVED', 'OPEN'] })
    const api = openTicket(agent, aTicket(), { 'PATCH /tickets/7/status': updated })
    const panel = await managePanel()

    await userEvent.setup().click(panel.getByRole('button', { name: 'In progress' }))

    await waitFor(() => expect(panel.getByRole('button', { name: 'Resolved' })).toBeInTheDocument())
    expect(api.callsTo('PATCH /tickets/7/status')[0].body).toEqual({ status: 'IN_PROGRESS' })
  })

  it('refreshes the history after a change', async () => {
    const api = openTicket(agent, aTicket(), { 'PATCH /tickets/7/status': aTicket({ status: 'CLOSED', nextStatuses: [] }) })
    const panel = await managePanel()
    await screen.findByText('No changes yet.')

    await userEvent.setup().click(panel.getByRole('button', { name: 'Closed' }))

    await waitFor(() => expect(api.callsTo('GET /tickets/7/history').length).toBeGreaterThan(1))
  })

  it('shows the server error when a move is rejected', async () => {
    openTicket(agent, aTicket(), {
      'PATCH /tickets/7/status': new Failure(409, 'Cannot move ticket from OPEN to CLOSED'),
    })
    const panel = await managePanel()

    await userEvent.setup().click(panel.getByRole('button', { name: 'Closed' }))

    expect(await panel.findByRole('alert')).toHaveTextContent('Cannot move ticket from OPEN to CLOSED')
  })

  it('sends only the changed override field', async () => {
    const api = openTicket(agent, aTicket(), { 'PATCH /tickets/7/classification': aTicket({ priority: 'LOW' }) })
    const panel = await managePanel()
    const user = userEvent.setup()
    expect(panel.getByRole('button', { name: 'Save override' })).toBeDisabled()

    await user.selectOptions(panel.getByLabelText('Priority'), 'LOW')
    await user.click(panel.getByRole('button', { name: 'Save override' }))

    await waitFor(() => expect(api.callsTo('PATCH /tickets/7/classification')).toHaveLength(1))
    expect(api.callsTo('PATCH /tickets/7/classification')[0].body).toEqual({ priority: 'LOW' })
  })

  it('assigns a ticket', async () => {
    const assigned = aTicket({ assigneeId: agent.id, assigneeEmail: agent.email })
    const api = openTicket(agent, aTicket(), { 'PATCH /tickets/7/assignee': assigned })
    const panel = await managePanel()
    await panel.findByRole('option', { name: agent.email })

    await userEvent.setup().selectOptions(panel.getByLabelText('Assignee'), agent.email)

    await waitFor(() => expect(api.callsTo('PATCH /tickets/7/assignee')).toHaveLength(1))
    expect(api.callsTo('PATCH /tickets/7/assignee')[0].body).toEqual({ assigneeId: agent.id })
  })

  it('removes an assignee', async () => {
    const assigned = aTicket({ assigneeId: agent.id, assigneeEmail: agent.email })
    const api = openTicket(agent, assigned, { 'PATCH /tickets/7/assignee': aTicket() })
    const panel = await managePanel()
    await panel.findByRole('option', { name: agent.email })

    await userEvent.setup().selectOptions(panel.getByLabelText('Assignee'), 'None')

    await waitFor(() => expect(api.callsTo('PATCH /tickets/7/assignee')).toHaveLength(1))
    expect(api.callsTo('PATCH /tickets/7/assignee')[0].body).toEqual({ assigneeId: null })
  })

  it('keeps showing an assignee who is no longer staff', async () => {
    openTicket(agent, aTicket({ assigneeId: 99, assigneeEmail: 'former@example.com' }))
    const panel = await managePanel()

    expect(await panel.findByRole('option', { name: 'former@example.com' })).toBeInTheDocument()
    expect(panel.getByLabelText('Assignee')).toHaveValue('99')
  })

  it('runs the classifier again', async () => {
    const api = openTicket(agent, aTicket(), { 'POST /tickets/7/reclassify': aTicket({ classifiedBy: 'RULES' }) })
    const panel = await managePanel()

    await userEvent.setup().click(panel.getByRole('button', { name: 'Run classifier again' }))

    expect(await screen.findByText('Rules')).toBeInTheDocument()
    expect(api.callsTo('POST /tickets/7/reclassify')).toHaveLength(1)
  })
})

describe('comments and history', () => {
  it('adds a comment, sending only the text', async () => {
    const api = openTicket(employee, aTicket(), {
      'POST /tickets/7/comments': { id: 1, authorEmail: employee.email, body: 'Any update?', createdAt: '2026-11-01T11:00:00Z' },
    })
    const user = userEvent.setup()
    await screen.findByText('No comments yet.')

    await user.type(screen.getByLabelText('Add a comment'), 'Any update?')
    await user.click(screen.getByRole('button', { name: 'Add comment' }))

    await waitFor(() => expect(screen.getByLabelText('Add a comment')).toHaveValue(''))
    expect(api.callsTo('POST /tickets/7/comments')[0].body).toEqual({ body: 'Any update?' })
    expect(api.callsTo('GET /tickets/7/comments').length).toBeGreaterThan(1)
  })

  it('lists comments with their author', async () => {
    openTicket(employee, aTicket(), {
      'GET /tickets/7/comments': [{ id: 1, authorEmail: agent.email, body: 'On it', createdAt: '2026-11-01T11:00:00Z' }],
    })

    expect(await screen.findByText('On it')).toBeInTheDocument()
    expect(screen.getByText(agent.email)).toBeInTheDocument()
  })

  it('shows history in plain words, and emails unchanged', async () => {
    openTicket(employee, aTicket(), {
      'GET /tickets/7/history': [
        { id: 1, field: 'status', oldValue: 'OPEN', newValue: 'IN_PROGRESS', changedBy: agent.email, changedAt: '2026-11-01T11:00:00Z' },
        { id: 2, field: 'assignee', oldValue: null, newValue: agent.email, changedBy: agent.email, changedAt: '2026-11-01T11:05:00Z' },
      ],
    })

    expect(await screen.findByText('Status: Open → In progress')).toBeInTheDocument()
    expect(screen.getByText(`Assignee: None → ${agent.email}`)).toBeInTheDocument()
  })
})
