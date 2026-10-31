import type { ReactNode } from 'react'
import { ClassifierBadge, PriorityBadge, StatusBadge } from './Badges.tsx'
import { LogoMark } from './Icons.tsx'
import { SlaMeter } from './SlaMeter.tsx'
import { useNow } from './useNow.ts'

const HOUR = 3_600_000

export function AuthLayout({ children }: { children: ReactNode }) {
  const now = useNow()
  const createdAt = new Date(now - 3 * HOUR).toISOString()
  const dueAt = new Date(now + 5 * HOUR).toISOString()

  return (
    <div className="auth">
      <aside className="auth-story">
        <span className="brand">
          <LogoMark />
          TicketWise
        </span>
        <div className="auth-pitch">
          <h2>Every IT problem, sorted before anyone reads it.</h2>
          <p>
            Describe the problem in plain words. AI suggests the category and priority, and agents take it from there,
            with every change on record.
          </p>
        </div>
        <div className="auth-sample" aria-hidden="true">
          <small>Example ticket</small>
          <div className="auth-sample-row">
            <span className="auth-sample-title">VPN keeps disconnecting</span>
            <ClassifierBadge source="GEMINI" />
          </div>
          <div className="auth-sample-row">
            <StatusBadge status="IN_PROGRESS" />
            <PriorityBadge priority="HIGH" />
          </div>
          <SlaMeter state="ON_TRACK" createdAt={createdAt} dueAt={dueAt} now={now} />
        </div>
      </aside>
      <main className="auth-form">
        <div className="auth-form-inner">{children}</div>
      </main>
    </div>
  )
}
