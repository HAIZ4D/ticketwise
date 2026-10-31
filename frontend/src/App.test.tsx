import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { tokenStore } from './api/client.ts'
import App from './App.tsx'
import { Failure, aPage, admin, agent, employee, logInAs, renderPage, stubApi } from './test/utils.tsx'

const noTickets = { 'GET /tickets': aPage([]) }

async function fillInLogin(email: string, password: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Log in' }))
}

describe('logging in', () => {
  it('sends logged-out visitors to the login page', async () => {
    stubApi({})

    renderPage(<App />, '/tickets')

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
  })

  it('stores the token and opens the tickets page', async () => {
    stubApi({ 'POST /auth/login': { token: 'new-token', user: employee }, ...noTickets })
    renderPage(<App />, '/login')

    await fillInLogin('aina@example.com', 'a-good-password')

    expect(await screen.findByRole('heading', { name: 'My tickets' })).toBeInTheDocument()
    expect(tokenStore.get()).toBe('new-token')
  })

  it('returns to the page the user first asked for', async () => {
    stubApi({ 'POST /auth/login': { token: 'new-token', user: admin }, 'GET /stats': new Failure(500, 'Stub') })
    renderPage(<App />, '/stats')

    await screen.findByRole('button', { name: 'Log in' })
    await fillInLogin('admin@example.com', 'a-good-password')

    expect(await screen.findByRole('heading', { name: 'Stats' })).toBeInTheDocument()
  })

  it('shows the server message on a failed login and stores nothing', async () => {
    stubApi({ 'POST /auth/login': new Failure(401, 'Invalid email or password') })
    renderPage(<App />, '/login')

    await fillInLogin('aina@example.com', 'wrong-password')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(tokenStore.get()).toBeNull()
  })

  it('registers and logs in, sending only the email and password', async () => {
    const api = stubApi({ 'POST /auth/register': { token: 'new-token', user: employee }, ...noTickets })
    const user = userEvent.setup()
    renderPage(<App />, '/register')

    await user.type(screen.getByLabelText('Email'), 'aina@example.com')
    await user.type(screen.getByLabelText(/Password/), 'a-good-password')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('heading', { name: 'My tickets' })).toBeInTheDocument()
    expect(api.callsTo('POST /auth/register')[0].body).toEqual({
      email: 'aina@example.com',
      password: 'a-good-password',
    })
  })
})

describe('staying logged in', () => {
  it('restores the user from the token after a reload', async () => {
    logInAs(employee, noTickets)

    renderPage(<App />, '/tickets')

    expect(await screen.findByText('aina@example.com')).toBeInTheDocument()
  })

  it('uses the current role from the server, not a stored one', async () => {
    logInAs({ ...agent, role: 'EMPLOYEE' }, noTickets)

    renderPage(<App />, '/tickets')

    expect(await screen.findByRole('heading', { name: 'My tickets' })).toBeInTheDocument()
  })

  it('returns to login when the token has expired', async () => {
    tokenStore.set('expired-token')
    stubApi({ 'GET /users/me': new Failure(401, 'You need to log in to do this') })

    renderPage(<App />, '/tickets')

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    expect(tokenStore.get()).toBeNull()
  })

  it('returns to login when a token expires in the middle of a session', async () => {
    logInAs(employee, { 'GET /tickets': new Failure(401, 'You need to log in to do this') })

    renderPage(<App />, '/tickets')

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    expect(tokenStore.get()).toBeNull()
  })

  it('logging out clears the token and all cached data', async () => {
    logInAs(employee, noTickets)
    const user = userEvent.setup()
    const { queryClient } = renderPage(<App />, '/tickets')
    await screen.findByRole('heading', { name: 'My tickets' })
    await waitFor(() => expect(queryClient.getQueryCache().getAll().length).toBeGreaterThan(0))

    await user.click(screen.getByRole('button', { name: 'Log out' }))

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    expect(tokenStore.get()).toBeNull()
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
  })
})

describe('role-based pages', () => {
  it.each([
    ['/stats', 'employee', employee],
    ['/users', 'employee', employee],
    ['/stats', 'agent', agent],
    ['/users', 'agent', agent],
  ])('blocks %s for an %s and asks the server for nothing', async (route, _name, user) => {
    const api = logInAs(user)

    renderPage(<App />, route)

    expect(await screen.findByRole('heading', { name: 'No access' })).toBeInTheDocument()
    expect(api.callsTo('GET /stats')).toHaveLength(0)
    expect(api.callsTo('GET /users')).toHaveLength(0)
  })

  it('shows admin links only to admins', async () => {
    logInAs(agent, noTickets)
    const { unmount } = renderPage(<App />, '/tickets')
    await screen.findByRole('link', { name: 'Tickets' })
    expect(screen.queryByRole('link', { name: 'Stats' })).not.toBeInTheDocument()
    unmount()

    logInAs(admin, noTickets)
    renderPage(<App />, '/tickets')

    expect(await screen.findByRole('link', { name: 'Stats' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Users' })).toBeInTheDocument()
  })

  it('shows a friendly page for an unknown URL', async () => {
    logInAs(employee)

    renderPage(<App />, '/nothing-here')

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
