import { useQuery } from '@tanstack/react-query'
import { api } from '../api/endpoints.ts'
import { CATEGORIES, PRIORITIES, STATUSES } from '../api/types.ts'
import type { Stats, TicketStatus } from '../api/types.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { SkeletonRows } from '../components/Skeleton.tsx'
import { count, label } from '../format.ts'

export function StatsPage() {
  const stats = useQuery({ queryKey: ['stats'], queryFn: api.getStats })

  return (
    <>
      <title>Stats · TicketWise</title>
      <h1>Stats</h1>
      <ErrorMessage error={stats.error} />
      {stats.isPending && <SkeletonRows label="Loading stats…" rows={3} />}
      {stats.data && <Dashboard stats={stats.data} />}
    </>
  )
}

function Dashboard({ stats }: { stats: Stats }) {
  const total = STATUSES.reduce((sum, status) => sum + stats.byStatus[status], 0)
  const priorities = [...PRIORITIES].reverse().map((priority) => ({
    name: priority,
    value: stats.byPriority[priority],
  }))
  const categories = CATEGORIES.map((category) => ({ name: category, value: stats.byCategory[category] })).sort(
    (first, second) => second.value - first.value,
  )

  return (
    <>
      <section className="stats-top">
        <div className="hero-figure">
          <h2>Overdue</h2>
          <p className={`hero-value ${stats.overdue > 0 ? 'is-alert' : ''}`}>{stats.overdue}</p>
          <p className="muted">{stats.overdue === 1 ? 'open ticket past its target' : 'open tickets past their target'}</p>
        </div>
        <StatusSplit stats={stats} total={total} />
      </section>
      <div className="stats-charts">
        <BarChart title="By priority" rows={priorities} highlight="CRITICAL" />
        <BarChart title="By category" rows={categories} />
      </div>
    </>
  )
}

function StatusSplit({ stats, total }: { stats: Stats; total: number }) {
  const parts = STATUSES.map((status) => ({ status, value: stats.byStatus[status] }))
  const summary = parts.map((part) => `${label(part.status)} ${part.value}`).join(', ')

  return (
    <section className="chart-group">
      <h2>{count(total, 'ticket')} by status</h2>
      <div className="split-bar" role="img" aria-label={summary}>
        {parts
          .filter((part) => part.value > 0)
          .map((part) => (
            <span key={part.status} className={partClass(part.status)} style={{ flexGrow: part.value }} />
          ))}
      </div>
      <ul className="split-list">
        {parts.map((part) => (
          <li key={part.status} className={partClass(part.status)}>
            <i aria-hidden="true" />
            <span>{label(part.status)}</span>
            <strong>{part.value}</strong>
          </li>
        ))}
      </ul>
    </section>
  )
}

function partClass(status: TicketStatus): string {
  return `part-${status.toLowerCase()}`
}

interface BarChartProps {
  title: string
  rows: { name: string; value: number }[]
  highlight?: string
}

function BarChart({ title, rows, highlight }: BarChartProps) {
  const largest = Math.max(...rows.map((row) => row.value), 1)

  return (
    <section className="chart-group">
      <h2>{title}</h2>
      <ul className="bars">
        {rows.map((row) => (
          <li key={row.name}>
            <span>{label(row.name)}</span>
            <span className="bar-track">
              <span
                className={`bar ${row.value === 0 ? 'is-empty' : ''} ${row.name === highlight ? 'is-critical' : ''}`}
                style={row.value > 0 ? { width: `${(row.value / largest) * 85}%` } : undefined}
              />
              <span className="bar-value">{row.value}</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}
