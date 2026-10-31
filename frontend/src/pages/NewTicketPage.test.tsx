import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from '../App.tsx'
import { Failure, aTicket, employee, logInAs, renderPage } from '../test/utils.tsx'

async function submitTicket() {
  const user = userEvent.setup()
  await screen.findByRole('heading', { name: 'Report a problem' })
  await user.type(screen.getByLabelText(/Title/), 'VPN keeps disconnecting')
  await user.type(screen.getByLabelText(/Description/), 'My VPN drops every 10 minutes')
  await user.click(screen.getByRole('button', { name: 'Submit ticket' }))
}

describe('new ticket', () => {
  it('sends only the title and description, then opens the ticket', async () => {
    const api = logInAs(employee, {
      'POST /tickets': aTicket(),
      'GET /tickets/7': aTicket(),
      'GET /tickets/7/comments': [],
      'GET /tickets/7/history': [],
    })
    renderPage(<App />, '/tickets/new')

    await submitTicket()

    expect(await screen.findByRole('heading', { name: 'VPN keeps disconnecting' })).toBeInTheDocument()
    expect(api.callsTo('POST /tickets')[0].body).toEqual({
      title: 'VPN keeps disconnecting',
      description: 'My VPN drops every 10 minutes',
    })
  })

  it('shows the errors from the server and stays on the form', async () => {
    logInAs(employee, {
      'POST /tickets': new Failure(400, 'Validation failed', [{ field: 'title', message: 'must not be blank' }]),
    })
    renderPage(<App />, '/tickets/new')

    await submitTicket()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Validation failed')
    expect(alert).toHaveTextContent('Title: must not be blank')
    expect(screen.getByRole('heading', { name: 'Report a problem' })).toBeInTheDocument()
  })
})
