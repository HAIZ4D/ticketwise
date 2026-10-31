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
    'GET /tickets/7/duplicates': [],
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

  it('shows the SLA state', async () => {
    openTicket(employee, aTicket({ slaState: 'DUE_SOON' }))

    expect(await screen.findByText('Due soon')).toBeInTheDocument()
    expect(screen.getByText('Target')).toBeInTheDocument()
  })

  it('shows everyone how a ticket was fixed', async () => {
    openTicket(employee, aTicket({ status: 'RESOLVED', resolution: 'Restarted the router' }))

    expect(await screen.findByRole('heading', { name: 'Resolution' })).toBeInTheDocument()
    expect(screen.getByText('Restarted the router')).toBeInTheDocument()
  })

  it('has no resolution section before a ticket is fixed', async () => {
    openTicket(employee)

    await screen.findByRole('heading', { name: 'VPN keeps disconnecting' })
    expect(screen.queryByRole('heading', { name: 'Resolution' })).not.toBeInTheDocument()
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

  it('asks how a ticket was fixed before resolving it', async () => {
    const before = aTicket({ status: 'IN_PROGRESS', nextStatuses: ['RESOLVED', 'OPEN'] })
    const after = aTicket({ status: 'RESOLVED', nextStatuses: ['CLOSED', 'IN_PROGRESS'], resolution: 'Restarted the router' })
    const api = openTicket(agent, before, { 'PATCH /tickets/7/status': after })
    const panel = await managePanel()
    const user = userEvent.setup()

    await user.click(panel.getByRole('button', { name: 'Resolved' }))
    expect(api.callsTo('PATCH /tickets/7/status')).toHaveLength(0)

    await user.type(panel.getByLabelText('How was it fixed?'), 'Restarted the router')
    await user.click(panel.getByRole('button', { name: 'Save and resolve' }))

    await waitFor(() => expect(api.callsTo('PATCH /tickets/7/status')).toHaveLength(1))
    expect(api.callsTo('PATCH /tickets/7/status')[0].body).toEqual({
      status: 'RESOLVED',
      resolution: 'Restarted the router',
    })
    await waitFor(() => expect(panel.queryByLabelText('How was it fixed?')).not.toBeInTheDocument())
  })

  it('can cancel resolving', async () => {
    const api = openTicket(agent, aTicket({ status: 'IN_PROGRESS', nextStatuses: ['RESOLVED', 'OPEN'] }))
    const panel = await managePanel()
    const user = userEvent.setup()

    await user.click(panel.getByRole('button', { name: 'Resolved' }))
    await user.click(panel.getByRole('button', { name: 'Cancel' }))

    expect(panel.queryByLabelText('How was it fixed?')).not.toBeInTheDocument()
    expect(api.callsTo('PATCH /tickets/7/status')).toHaveLength(0)
  })

  it('keeps the note when resolving fails, so it is not lost', async () => {
    openTicket(agent, aTicket({ status: 'IN_PROGRESS', nextStatuses: ['RESOLVED', 'OPEN'] }), {
      'PATCH /tickets/7/status': new Failure(400, 'A resolution note is required to resolve a ticket'),
    })
    const panel = await managePanel()
    const user = userEvent.setup()

    await user.click(panel.getByRole('button', { name: 'Resolved' }))
    await user.type(panel.getByLabelText('How was it fixed?'), 'Tried something')
    await user.click(panel.getByRole('button', { name: 'Save and resolve' }))

    expect(await panel.findByRole('alert')).toHaveTextContent('A resolution note is required')
    expect(panel.getByLabelText('How was it fixed?')).toHaveValue('Tried something')
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

describe('similar tickets', () => {
  const similar = { id: 12, title: 'VPN drops at home', status: 'OPEN', score: 0.914 }

  it('shows staff possible duplicates with a link and a score', async () => {
    openTicket(agent, aTicket(), { 'GET /tickets/7/duplicates': [similar] })

    const panel = within(await screen.findByRole('region', { name: 'Possible duplicates' }))
    expect(panel.getByRole('link', { name: '#12 VPN drops at home' })).toHaveAttribute('href', '/tickets/12')
    expect(panel.getByText(/91% similar/)).toBeInTheDocument()
  })

  it('shows no duplicates panel when there are none', async () => {
    openTicket(agent)

    await screen.findByRole('region', { name: 'Suggested fix' })
    expect(screen.queryByRole('region', { name: 'Possible duplicates' })).not.toBeInTheDocument()
  })

  it('never shows employees these tools, or asks the server for them', async () => {
    const api = openTicket(employee)

    await screen.findByRole('heading', { name: 'VPN keeps disconnecting' })
    expect(screen.queryByRole('region', { name: 'Suggested fix' })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Possible duplicates' })).not.toBeInTheDocument()
    expect(api.callsTo('GET /tickets/7/duplicates')).toHaveLength(0)
  })

  it('asks for a suggestion only when the button is clicked', async () => {
    const api = openTicket(agent, aTicket(), {
      'POST /tickets/7/suggestion': { suggestion: 'Update the VPN client.', sources: [similar] },
    })
    const panel = within(await screen.findByRole('region', { name: 'Suggested fix' }))
    expect(api.callsTo('POST /tickets/7/suggestion')).toHaveLength(0)

    await userEvent.setup().click(panel.getByRole('button', { name: 'Suggest a fix' }))

    expect(await panel.findByText('Update the VPN client.')).toBeInTheDocument()
    expect(panel.getByText('AI suggestion. Check it before you use it.')).toBeInTheDocument()
    expect(panel.getByRole('heading', { name: 'Based on' })).toBeInTheDocument()
    expect(panel.getByRole('link', { name: '#12 VPN drops at home' })).toBeInTheDocument()
    expect(api.callsTo('POST /tickets/7/suggestion')).toHaveLength(1)
  })

  it('says so when nothing similar has been solved', async () => {
    openTicket(agent, aTicket(), { 'POST /tickets/7/suggestion': { suggestion: null, sources: [] } })
    const panel = within(await screen.findByRole('region', { name: 'Suggested fix' }))

    await userEvent.setup().click(panel.getByRole('button', { name: 'Suggest a fix' }))

    expect(await panel.findByText('No similar solved tickets yet.')).toBeInTheDocument()
  })

  it('still lists similar tickets when there is no AI advice', async () => {
    openTicket(agent, aTicket(), { 'POST /tickets/7/suggestion': { suggestion: null, sources: [similar] } })
    const panel = within(await screen.findByRole('region', { name: 'Suggested fix' }))

    await userEvent.setup().click(panel.getByRole('button', { name: 'Suggest a fix' }))

    expect(await panel.findByRole('heading', { name: 'Similar solved tickets' })).toBeInTheDocument()
    expect(panel.queryByText('AI suggestion. Check it before you use it.')).not.toBeInTheDocument()
  })

  it('shows the server message when suggestions are unavailable', async () => {
    openTicket(agent, aTicket(), {
      'POST /tickets/7/suggestion': new Failure(503, 'Suggestions are unavailable right now. Please try again later.'),
    })
    const panel = within(await screen.findByRole('region', { name: 'Suggested fix' }))

    await userEvent.setup().click(panel.getByRole('button', { name: 'Suggest a fix' }))

    expect(await panel.findByRole('alert')).toHaveTextContent('Suggestions are unavailable right now')
  })

  it('shows AI text as plain text, never as HTML', async () => {
    const attack = '<img src=x onerror=alert(1)>'
    openTicket(agent, aTicket(), { 'POST /tickets/7/suggestion': { suggestion: attack, sources: [similar] } })
    const panel = within(await screen.findByRole('region', { name: 'Suggested fix' }))

    await userEvent.setup().click(panel.getByRole('button', { name: 'Suggest a fix' }))

    expect(await panel.findByText(attack)).toBeInTheDocument()
    expect(document.querySelector('img')).toBeNull()
  })

  it('starts fresh when returning to a ticket that is already loaded', async () => {
    const back = { id: 7, title: 'VPN keeps disconnecting', status: 'OPEN', score: 0.914 }
    openTicket(agent, aTicket(), {
      'GET /tickets/7/duplicates': [similar],
      'GET /tickets/12': aTicket({ id: 12, title: 'VPN drops at home' }),
      'GET /tickets/12/comments': [],
      'GET /tickets/12/history': [],
      'GET /tickets/12/duplicates': [back],
      'POST /tickets/12/suggestion': { suggestion: 'Update the VPN client.', sources: [back] },
    })
    const user = userEvent.setup()
    const duplicates = async () => within(await screen.findByRole('region', { name: 'Possible duplicates' }))
    await user.click((await duplicates()).getByRole('link'))
    await screen.findByRole('heading', { name: 'VPN drops at home' })

    const suggestion = within(await screen.findByRole('region', { name: 'Suggested fix' }))
    await user.type(screen.getByLabelText('Add a comment'), 'A half-written note')
    await user.click(suggestion.getByRole('button', { name: 'Suggest a fix' }))
    await suggestion.findByText('Update the VPN client.')
    await user.click((await duplicates()).getByRole('link'))

    expect(await screen.findByRole('heading', { name: 'VPN keeps disconnecting' })).toBeInTheDocument()
    expect(screen.queryByText('Update the VPN client.')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Add a comment')).toHaveValue('')
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
