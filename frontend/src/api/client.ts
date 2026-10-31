import type { FieldError } from './types.ts'

const TOKEN_KEY = 'ticketwise.token'

export class ApiError extends Error {
  status: number
  fieldErrors: FieldError[]

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

let onSessionExpired = () => {}

export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler
}

export async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const token = tokenStore.get()
  const headers: Record<string, string> = {}
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await send(`/api/v1${path}`, { method, headers, body: JSON.stringify(body) })

  if (response.status === 401 && token) {
    onSessionExpired()
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  return (await response.json()) as T
}

async function send(url: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(url, init)
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Please check your connection and try again.')
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = await response.json()
    return new ApiError(response.status, body.message, body.fieldErrors)
  } catch {
    return new ApiError(response.status, 'Something went wrong. Please try again later.')
  }
}
