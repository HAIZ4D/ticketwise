import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { setSessionExpiredHandler, tokenStore } from '../api/client.ts'
import { api } from '../api/endpoints.ts'
import type { AuthResponse, User } from '../api/types.ts'
import { AuthContext } from './useAuth.ts'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)

  const logout = useCallback(() => {
    tokenStore.clear()
    queryClient.clear()
    setUser(null)
  }, [queryClient])

  const startSession = useCallback(
    (auth: AuthResponse) => {
      tokenStore.set(auth.token)
      queryClient.clear()
      setUser(auth.user)
    },
    [queryClient],
  )

  useEffect(() => {
    setSessionExpiredHandler(logout)
  }, [logout])

  useEffect(() => {
    if (!tokenStore.get()) {
      return
    }
    api
      .me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      logout,
      login: async (email: string, password: string) => startSession(await api.login(email, password)),
      register: async (email: string, password: string) => startSession(await api.register(email, password)),
    }),
    [user, loading, logout, startSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
