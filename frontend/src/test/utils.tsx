import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import { vi } from 'vitest'
import { tokenStore } from '../api/client.ts'
import type { Ticket, User } from '../api/types.ts'
import { AuthProvider } from '../auth/AuthContext.tsx'

export const employee: User = { id: 1, email: 'aina@example.com', role: 'EMPLOYEE', createdAt: '2026-11-01T10:00:00Z' }
export const agent: User = { id: 2, email: 'agent@example.com', role: 'AGENT', createdAt: '2026-11-01T10:00:00Z' }
export const admin: User = { id: 3, email: 'admin@example.com', role: 'ADMIN', createdAt: '2026-11-01T10:00:00Z' }

export function aTicket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: 7,
    title: 'VPN keeps disconnecting',
    description: 'My VPN drops every 10 minutes',
    requesterEmail: employee.email,
    status: 'OPEN',
    nextStatuses: ['IN_PROGRESS', 'CLOSED'],
    category: 'NETWORK',
    priority: 'HIGH',
    aiCategory: 'NETWORK',
    aiPriority: 'HIGH',
    aiReason: 'Repeated VPN drops block work.',
    classifiedBy: 'GEMINI',
    overridden: false,
    assigneeId: null,
    assigneeEmail: null,
    createdAt: '2026-11-01T10:15:00Z',
    updatedAt: '2026-11-01T10:15:00Z',
    resolvedAt: null,
    ...overrides,
  }
}

export function aPage<T>(content: T[], overrides = {}) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, ...overrides }
}

export class Failure {
  status: number
  body: unknown

  constructor(status: number, message: string, fieldErrors: unknown[] = []) {
    this.status = status
    this.body = { status, message, fieldErrors }
  }
}

export interface ApiCall {
  key: string
  query: URLSearchParams
  body: unknown
  headers: Record<string, string>
}

export function stubApi(routes: Record<string, unknown>) {
  const calls: ApiCall[] = []
  const fetchStub = vi.fn(async (url: string, init: RequestInit = {}) => {
    const [path, query = ''] = url.replace('/api/v1', '').split('?')
    const key = `${init.method ?? 'GET'} ${path}`
    calls.push({
      key,
      query: new URLSearchParams(query),
      body: init.body ? JSON.parse(init.body as string) : undefined,
      headers: (init.headers ?? {}) as Record<string, string>,
    })
    const reply = key in routes ? routes[key] : new Failure(404, `No stub for ${key}`)
    if (reply instanceof Failure) {
      return new Response(JSON.stringify(reply.body), { status: reply.status })
    }
    return new Response(JSON.stringify(reply), { status: 200 })
  })
  vi.stubGlobal('fetch', fetchStub)
  return { calls, callsTo: (key: string) => calls.filter((call) => call.key === key) }
}

export function logInAs(user: User, routes: Record<string, unknown> = {}) {
  tokenStore.set('test-token')
  return stubApi({ 'GET /users/me': user, ...routes })
}

export function renderPage(page: ReactElement, route = '/') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const view = render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <AuthProvider>{page}</AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { ...view, queryClient }
}
