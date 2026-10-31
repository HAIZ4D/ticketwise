import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App.tsx'
import { aPage, aTicket, agent, employee, logInAs, renderPage } from '../test/utils.tsx'

const onePage = { 'GET /tickets': aPage([aTicket()]) }

describe('ticket list', () => {
  it('shows tickets with their badges', async () => {
    logInAs(employee, onePage)

    renderPage(<App />, '/tickets')

    expect(await screen.findByRole('link', { name: 'VPN keeps disconnecting' })).toHaveAttribute('href', '/tickets/7')
    const table = within(screen.getByRole('table'))
    expect(table.getByText('High')).toBeInTheDocument()
    expect(table.getByText('AI')).toBeInTheDocument()
  })

  it('counts tickets in correct English', async () => {
    logInAs(employee, onePage)
    const { unmount } = renderPage(<App />, '/tickets')
    expect(await screen.findByText(/· 1 ticket$/)).toBeInTheDocument()
    unmount()

    logInAs(employee, { 'GET /tickets': aPage([aTicket()], { totalElements: 2 }) })
    renderPage(<App />, '/tickets')

    expect(await screen.findByText(/· 2 tickets$/)).toBeInTheDocument()
  })

  it('shows a message when nothing matches', async () => {
    logInAs(employee, { 'GET /tickets': aPage([]) })

    renderPage(<App />, '/tickets')

    expect(await screen.findByText('No tickets match these filters.')).toBeInTheDocument()
  })

  it('sends the chosen filter to the server', async () => {
    const api = logInAs(employee, onePage)
    const user = userEvent.setup()
    renderPage(<App />, '/tickets')
    await screen.findByRole('link', { name: 'VPN keeps disconnecting' })

    await user.selectOptions(screen.getByLabelText('Status'), 'OPEN')

    await waitFor(() => expect(api.callsTo('GET /tickets').at(-1)?.query.get('status')).toBe('OPEN'))
  })

  it('reads filters from the URL, so a filtered view can be shared', async () => {
    const api = logInAs(employee, onePage)

    renderPage(<App />, '/tickets?priority=HIGH&category=NETWORK')

    await screen.findByRole('link', { name: 'VPN keeps disconnecting' })
    const query = api.callsTo('GET /tickets')[0].query
    expect(query.get('priority')).toBe('HIGH')
    expect(query.get('category')).toBe('NETWORK')
    expect(screen.getByLabelText('Priority')).toHaveValue('HIGH')
  })

  it('moves between pages', async () => {
    const api = logInAs(employee, {
      'GET /tickets': aPage([aTicket()], { totalElements: 45, totalPages: 3 }),
    })
    const user = userEvent.setup()
    renderPage(<App />, '/tickets')
    await screen.findByText(/Page 1 of 3/)
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Next' }))

    await waitFor(() => expect(api.callsTo('GET /tickets').at(-1)?.query.get('page')).toBe('1'))
    expect(await screen.findByText(/Page 2 of 3/)).toBeInTheDocument()
  })

  it('goes back to the first page when a filter changes', async () => {
    const api = logInAs(employee, {
      'GET /tickets': aPage([aTicket()], { totalElements: 45, totalPages: 3 }),
    })
    const user = userEvent.setup()
    renderPage(<App />, '/tickets?page=3')
    await screen.findByText(/Page 3 of 3/)

    await user.selectOptions(screen.getByLabelText('Priority'), 'LOW')

    await waitFor(() => expect(api.callsTo('GET /tickets').at(-1)?.query.get('page')).toBe('0'))
  })

  it('gives staff a My queue option that filters by their id', async () => {
    const api = logInAs(agent, onePage)
    const user = userEvent.setup()
    renderPage(<App />, '/tickets')
    await screen.findByRole('heading', { name: 'All tickets' })

    await user.click(screen.getByLabelText('My queue'))

    await waitFor(() => expect(api.callsTo('GET /tickets').at(-1)?.query.get('assigneeId')).toBe(String(agent.id)))
  })

  it('does not offer My queue to employees, even through the URL', async () => {
    const api = logInAs(employee, onePage)

    renderPage(<App />, '/tickets?mine=1')

    await screen.findByRole('heading', { name: 'My tickets' })
    expect(screen.queryByLabelText('My queue')).not.toBeInTheDocument()
    await waitFor(() => expect(api.callsTo('GET /tickets').length).toBeGreaterThan(0))
    expect(api.callsTo('GET /tickets')[0].query.has('assigneeId')).toBe(false)
  })

  it('gives staff an overdue filter', async () => {
    const api = logInAs(agent, onePage)
    const user = userEvent.setup()
    renderPage(<App />, '/tickets')
    await screen.findByRole('heading', { name: 'All tickets' })
    expect(api.callsTo('GET /tickets')[0].query.has('overdue')).toBe(false)

    await user.click(screen.getByLabelText('Overdue only'))

    await waitFor(() => expect(api.callsTo('GET /tickets').at(-1)?.query.get('overdue')).toBe('true'))
  })

  it('does not offer the overdue filter to employees, even through the URL', async () => {
    const api = logInAs(employee, onePage)

    renderPage(<App />, '/tickets?overdue=1')

    await screen.findByRole('heading', { name: 'My tickets' })
    expect(screen.queryByLabelText('Overdue only')).not.toBeInTheDocument()
    await waitFor(() => expect(api.callsTo('GET /tickets').length).toBeGreaterThan(0))
    expect(api.callsTo('GET /tickets')[0].query.has('overdue')).toBe(false)
  })

  it('shows the SLA state of each ticket', async () => {
    logInAs(agent, { 'GET /tickets': aPage([aTicket({ slaState: 'OVERDUE' })]) })

    renderPage(<App />, '/tickets')

    expect(await screen.findByText('Overdue')).toBeInTheDocument()
  })

  it('hides the requester column from employees', async () => {
    logInAs(employee, onePage)

    renderPage(<App />, '/tickets')

    await screen.findByRole('link', { name: 'VPN keeps disconnecting' })
    expect(screen.queryByRole('columnheader', { name: 'Requester' })).not.toBeInTheDocument()
  })
})
