import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Failure, stubApi } from '../test/utils.tsx'
import { ApiError, request, setSessionExpiredHandler, tokenStore } from './client.ts'

const failureOf = (call: Promise<unknown>) => call.then(() => null, (error: ApiError) => error) as Promise<ApiError>

describe('request', () => {
  const sessionExpired = vi.fn()

  beforeEach(() => {
    sessionExpired.mockClear()
    setSessionExpiredHandler(sessionExpired)
  })

  it('sends the token when logged in', async () => {
    tokenStore.set('my-token')
    const api = stubApi({ 'GET /tickets': [] })

    await request('/tickets')

    expect(api.calls[0].headers.Authorization).toBe('Bearer my-token')
  })

  it('sends no token when logged out', async () => {
    const api = stubApi({ 'GET /tickets': [] })

    await request('/tickets')

    expect(api.calls[0].headers.Authorization).toBeUndefined()
  })

  it('sends a JSON body', async () => {
    const api = stubApi({ 'POST /tickets': {} })

    await request('/tickets', 'POST', { title: 'Hello' })

    expect(api.calls[0].headers['Content-Type']).toBe('application/json')
    expect(api.calls[0].body).toEqual({ title: 'Hello' })
  })

  it('turns an error response into an ApiError', async () => {
    const fieldErrors = [{ field: 'title', message: 'must not be blank' }]
    stubApi({ 'POST /tickets': new Failure(400, 'Validation failed', fieldErrors) })

    const error = await failureOf(request('/tickets', 'POST', {}))

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('Validation failed')
    expect(error.fieldErrors).toEqual(fieldErrors)
  })

  it('copes with an error response that is not JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('<html>Bad gateway</html>', { status: 502 })))

    const error = await failureOf(request('/tickets'))

    expect(error.status).toBe(502)
    expect(error.message).toBe('Something went wrong. Please try again later.')
  })

  it('explains when the server cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))))

    const error = await failureOf(request('/tickets'))

    expect(error).toBeInstanceOf(ApiError)
    expect(error.message).toContain('Cannot reach the server')
  })

  it('ends the session when a token is rejected', async () => {
    tokenStore.set('expired-token')
    stubApi({ 'GET /tickets': new Failure(401, 'You need to log in to do this') })

    await request('/tickets').catch(() => {})

    expect(sessionExpired).toHaveBeenCalledOnce()
  })

  it('does not end a session on a failed login, which has no token', async () => {
    stubApi({ 'POST /auth/login': new Failure(401, 'Invalid email or password') })

    await request('/auth/login', 'POST', {}).catch(() => {})

    expect(sessionExpired).not.toHaveBeenCalled()
  })

  it('does not end the session on a 403', async () => {
    tokenStore.set('good-token')
    stubApi({ 'GET /stats': new Failure(403, 'You do not have permission to do this') })

    await request('/stats').catch(() => {})

    expect(sessionExpired).not.toHaveBeenCalled()
  })
})
