import { createContext, useContext } from 'react'
import type { User } from '../api/types.ts'

export interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return value
}

export function useCurrentUser(): User {
  const { user } = useAuth()
  if (!user) {
    throw new Error('useCurrentUser must be used inside RequireAuth')
  }
  return user
}

export function isStaff(user: User): boolean {
  return user.role === 'AGENT' || user.role === 'ADMIN'
}
