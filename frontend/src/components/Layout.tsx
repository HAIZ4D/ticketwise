import { ChartColumn, Inbox, Plus, Users } from 'lucide-react'
import { Link, NavLink, Outlet } from 'react-router'
import { useAuth, useCurrentUser } from '../auth/useAuth.ts'
import { label } from '../format.ts'
import { LogoMark } from './Icons.tsx'
import { ThemeToggle } from './ThemeToggle.tsx'

export function Layout() {
  const { logout } = useAuth()
  const user = useCurrentUser()

  return (
    <div className="shell">
      <aside className="rail">
        <Link className="brand" to="/tickets">
          <LogoMark />
          TicketWise
        </Link>
        <nav aria-label="Main">
          <NavLink to="/tickets" end>
            <Inbox size={16} aria-hidden="true" />
            Tickets
          </NavLink>
          <NavLink to="/tickets/new">
            <Plus size={16} aria-hidden="true" />
            New ticket
          </NavLink>
          {user.role === 'ADMIN' && (
            <NavLink to="/stats">
              <ChartColumn size={16} aria-hidden="true" />
              Stats
            </NavLink>
          )}
          {user.role === 'ADMIN' && (
            <NavLink to="/users">
              <Users size={16} aria-hidden="true" />
              Users
            </NavLink>
          )}
        </nav>
        <div className="rail-foot">
          <div className="account">
            <span className="account-email">{user.email}</span>
            <span className="account-role">{label(user.role)}</span>
          </div>
          <div className="rail-actions">
            <ThemeToggle />
            <button type="button" onClick={logout}>
              Log out
            </button>
          </div>
        </div>
      </aside>
      <main className="page">
        <Outlet />
      </main>
    </div>
  )
}
