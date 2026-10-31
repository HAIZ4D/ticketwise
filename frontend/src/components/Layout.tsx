import { Link, NavLink, Outlet } from 'react-router'
import { useAuth, useCurrentUser } from '../auth/useAuth.ts'
import { label } from '../format.ts'

export function Layout() {
  const { logout } = useAuth()
  const user = useCurrentUser()

  return (
    <>
      <header className="topbar">
        <Link className="brand" to="/tickets">
          TicketWise
        </Link>
        <nav aria-label="Main">
          <NavLink to="/tickets" end>
            Tickets
          </NavLink>
          <NavLink to="/tickets/new">New ticket</NavLink>
          {user.role === 'ADMIN' && <NavLink to="/stats">Stats</NavLink>}
          {user.role === 'ADMIN' && <NavLink to="/users">Users</NavLink>}
        </nav>
        <div className="account">
          <span className="account-email">{user.email}</span>
          <span className="badge">{label(user.role)}</span>
          <button type="button" className="button-quiet" onClick={logout}>
            Log out
          </button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
