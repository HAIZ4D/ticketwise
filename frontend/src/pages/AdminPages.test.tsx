import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App.tsx'
import { Failure, aPage, admin, employee, logInAs, renderPage } from '../test/utils.tsx'

describe('stats page', () => {
  it('shows a card for every count', async () => {
    logInAs(admin, {
      'GET /stats': {
        byStatus: { OPEN: 3, IN_PROGRESS: 2, RESOLVED: 1, CLOSED: 0 },
        byPriority: { LOW: 1, MEDIUM: 2, HIGH: 2, CRITICAL: 1 },
        byCategory: { NETWORK: 4, HARDWARE: 1, SOFTWARE: 1, ACCESS: 0, EMAIL: 0, OTHER: 0 },
        overdue: 9,
      },
    })

    renderPage(<App />, '/stats')

    expect(await screen.findByText('In progress')).toBeInTheDocument()
    expect(screen.getByText('Overdue')).toBeInTheDocument()
    expect(screen.getByText('9')).toBeInTheDocument()
    expect(screen.getByText('Network')).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()
    expect(screen.getByText('Critical')).toBeInTheDocument()
  })
})

describe('users page', () => {
  const users = aPage([admin, employee])

  it('changes a role', async () => {
    const api = logInAs(admin, { 'GET /users': users, 'PATCH /users/1/role': { ...employee, role: 'AGENT' } })
    renderPage(<App />, '/users')
    const select = await screen.findByLabelText(`Role for ${employee.email}`)

    await userEvent.setup().selectOptions(select, 'AGENT')

    await waitFor(() => expect(api.callsTo('PATCH /users/1/role')).toHaveLength(1))
    expect(api.callsTo('PATCH /users/1/role')[0].body).toEqual({ role: 'AGENT' })
  })

  it('does not let an admin change their own role', async () => {
    logInAs(admin, { 'GET /users': users })

    renderPage(<App />, '/users')

    expect(await screen.findByLabelText(`Role for ${admin.email}`)).toBeDisabled()
    expect(screen.getByLabelText(`Role for ${employee.email}`)).toBeEnabled()
  })

  it('shows the server error when a change is rejected', async () => {
    logInAs(admin, { 'GET /users': users, 'PATCH /users/1/role': new Failure(403, 'You do not have permission to do this') })
    renderPage(<App />, '/users')
    const select = await screen.findByLabelText(`Role for ${employee.email}`)

    await userEvent.setup().selectOptions(select, 'ADMIN')

    expect(await screen.findByRole('alert')).toHaveTextContent('You do not have permission to do this')
  })
})
