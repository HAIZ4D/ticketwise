import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'
import { AuthLayout } from '../components/AuthLayout.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'

export function LoginPage() {
  const { user, login } = useAuth()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const signIn = useMutation({ mutationFn: () => login(email, password) })

  if (user) {
    return <Navigate to={location.state?.from ?? '/tickets'} replace />
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    signIn.mutate()
  }

  return (
    <AuthLayout>
      <title>Log in · TicketWise</title>
      <header>
        <h1>Welcome back</h1>
        <p className="muted">Log in to report and track IT problems.</p>
      </header>
      <form onSubmit={handleSubmit}>
        <label>
          Email
          <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Password
          <input
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <ErrorMessage error={signIn.error} />
        <button type="submit" disabled={signIn.isPending}>
          {signIn.isPending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="muted">
        New here? <Link to="/register">Create an account</Link>
      </p>
    </AuthLayout>
  )
}
