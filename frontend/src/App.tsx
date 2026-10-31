import { Link, Navigate, Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth.tsx'
import { Layout } from './components/Layout.tsx'
import { LoginPage } from './pages/LoginPage.tsx'
import { NewTicketPage } from './pages/NewTicketPage.tsx'
import { RegisterPage } from './pages/RegisterPage.tsx'
import { StatsPage } from './pages/StatsPage.tsx'
import { TicketDetailPage } from './pages/TicketDetailPage.tsx'
import { TicketListPage } from './pages/TicketListPage.tsx'
import { UsersPage } from './pages/UsersPage.tsx'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/tickets" replace />} />
          <Route path="/tickets" element={<TicketListPage />} />
          <Route path="/tickets/new" element={<NewTicketPage />} />
          <Route path="/tickets/:id" element={<TicketDetailPage />} />
          <Route element={<RequireAuth roles={['ADMIN']} />}>
            <Route path="/stats" element={<StatsPage />} />
            <Route path="/users" element={<UsersPage />} />
          </Route>
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  )
}

function NotFound() {
  return (
    <section>
      <h1>Page not found</h1>
      <p>
        <Link to="/tickets">Back to tickets</Link>
      </p>
    </section>
  )
}
