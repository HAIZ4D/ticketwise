import { Navigate, Outlet, useLocation } from 'react-router'
import type { UserRole } from '../api/types.ts'
import { useAuth } from './useAuth.ts'

export function RequireAuth({ roles }: { roles?: UserRole[] }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <p className="status-text">Loading…</p>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (roles && !roles.includes(user.role)) {
    return (
      <section>
        <h1>No access</h1>
        <p>You don't have permission to view this page.</p>
      </section>
    )
  }
  return <Outlet />
}
