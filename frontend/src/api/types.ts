export type UserRole = 'EMPLOYEE' | 'AGENT' | 'ADMIN'
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type TicketCategory = 'NETWORK' | 'HARDWARE' | 'SOFTWARE' | 'ACCESS' | 'EMAIL' | 'OTHER'
export type ClassifierSource = 'GEMINI' | 'RULES'
export type SlaState = 'ON_TRACK' | 'DUE_SOON' | 'OVERDUE' | 'MET' | 'MISSED' | 'NONE'

export const ROLES: UserRole[] = ['EMPLOYEE', 'AGENT', 'ADMIN']
export const STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']
export const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
export const CATEGORIES: TicketCategory[] = ['NETWORK', 'HARDWARE', 'SOFTWARE', 'ACCESS', 'EMAIL', 'OTHER']

export interface User {
  id: number
  email: string
  role: UserRole
  createdAt: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface Ticket {
  id: number
  title: string
  description: string
  requesterEmail: string
  status: TicketStatus
  nextStatuses: TicketStatus[]
  category: TicketCategory
  priority: TicketPriority
  aiCategory: TicketCategory
  aiPriority: TicketPriority
  aiReason: string
  classifiedBy: ClassifierSource
  overridden: boolean
  assigneeId: number | null
  assigneeEmail: string | null
  slaDueAt: string
  slaState: SlaState
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
  resolution: string | null
}

export interface Comment {
  id: number
  authorEmail: string
  body: string
  createdAt: string
}

export interface HistoryEntry {
  id: number
  field: string
  oldValue: string | null
  newValue: string | null
  changedBy: string
  changedAt: string
}

export interface SimilarTicket {
  id: number
  title: string
  status: TicketStatus
  score: number
}

export interface Suggestion {
  suggestion: string | null
  sources: SimilarTicket[]
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Stats {
  byStatus: Record<TicketStatus, number>
  byCategory: Record<TicketCategory, number>
  byPriority: Record<TicketPriority, number>
  overdue: number
}

export interface FieldError {
  field: string
  message: string
}
