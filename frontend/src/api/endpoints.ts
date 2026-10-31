import { request } from './client.ts'
import type {
  AuthResponse,
  Comment,
  HistoryEntry,
  Page,
  SimilarTicket,
  Stats,
  Suggestion,
  Ticket,
  TicketCategory,
  TicketPriority,
  TicketStatus,
  User,
  UserRole,
} from './types.ts'

export interface TicketFilters {
  status?: string
  priority?: string
  category?: string
  assigneeId?: number
  overdue?: boolean
  sort?: string
  page?: number
}

export interface Classification {
  category?: TicketCategory
  priority?: TicketPriority
}

export const api = {
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', 'POST', { email, password }),
  register: (email: string, password: string) =>
    request<AuthResponse>('/auth/register', 'POST', { email, password }),
  me: () => request<User>('/users/me'),

  listTickets: (filters: TicketFilters) => request<Page<Ticket>>(`/tickets?${toQuery(filters)}`),
  getTicket: (id: string) => request<Ticket>(`/tickets/${id}`),
  createTicket: (title: string, description: string) =>
    request<Ticket>('/tickets', 'POST', { title, description }),
  changeStatus: (id: number, status: TicketStatus, resolution?: string) =>
    request<Ticket>(`/tickets/${id}/status`, 'PATCH', { status, resolution }),
  overrideClassification: (id: number, classification: Classification) =>
    request<Ticket>(`/tickets/${id}/classification`, 'PATCH', classification),
  assign: (id: number, assigneeId: number | null) =>
    request<Ticket>(`/tickets/${id}/assignee`, 'PATCH', { assigneeId }),
  reclassify: (id: number) => request<Ticket>(`/tickets/${id}/reclassify`, 'POST'),

  listComments: (ticketId: string) => request<Comment[]>(`/tickets/${ticketId}/comments`),
  addComment: (ticketId: string, body: string) =>
    request<Comment>(`/tickets/${ticketId}/comments`, 'POST', { body }),
  listHistory: (ticketId: string) => request<HistoryEntry[]>(`/tickets/${ticketId}/history`),

  listDuplicates: (ticketId: string) => request<SimilarTicket[]>(`/tickets/${ticketId}/duplicates`),
  suggestFix: (ticketId: string) => request<Suggestion>(`/tickets/${ticketId}/suggestion`, 'POST'),

  listAgents: () => request<User[]>('/users/agents'),
  listUsers: (page: number) => request<Page<User>>(`/users?page=${page}`),
  changeRole: (id: number, role: UserRole) => request<User>(`/users/${id}/role`, 'PATCH', { role }),

  getStats: () => request<Stats>('/stats'),
}

function toQuery(filters: TicketFilters): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  }
  return query.toString()
}
