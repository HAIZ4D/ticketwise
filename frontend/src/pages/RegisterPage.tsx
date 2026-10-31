import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'
import { AuthLayout } from '../components/AuthLayout.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'

export function RegisterPage() {
  const { user, register } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const signUp = useMutation({ mutationFn: () => register(email, password) })

  if (user) {
    return <Navigate to="/tickets" replace />
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    signUp.mutate()
  }

  return (
    <AuthLayout>
      <title>Create an account · TicketWise</title>
      <header>
        <h1>Create an account</h1>
        <p className="muted">You'll be able to report problems and track your tickets.</p>
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
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
            aria-describedby="password-help"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <small id="password-help">8 to 72 characters.</small>
        </label>
        <ErrorMessage error={signUp.error} />
        <button type="submit" disabled={signUp.isPending}>
          {signUp.isPending ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="muted">
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </AuthLayout>
  )
}
