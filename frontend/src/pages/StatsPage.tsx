import { useQuery } from '@tanstack/react-query'
import { api } from '../api/endpoints.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { label } from '../format.ts'

export function StatsPage() {
  const stats = useQuery({ queryKey: ['stats'], queryFn: api.getStats })

  return (
    <>
      <title>Stats · TicketWise</title>
      <h1>Stats</h1>
      <ErrorMessage error={stats.error} />
      {stats.isPending && <p className="status-text">Loading stats…</p>}
      {stats.data && (
        <>
          <StatGroup title="Service targets" counts={{ OVERDUE: stats.data.overdue }} />
          <StatGroup title="By status" counts={stats.data.byStatus} />
          <StatGroup title="By priority" counts={stats.data.byPriority} />
          <StatGroup title="By category" counts={stats.data.byCategory} />
        </>
      )}
    </>
  )
}

function StatGroup({ title, counts }: { title: string; counts: Record<string, number> }) {
  return (
    <section>
      <h2>{title}</h2>
      <div className="stat-grid">
        {Object.entries(counts).map(([name, count]) => (
          <div key={name} className="card stat">
            <p className="stat-value">{count}</p>
            <p className="muted">{label(name)}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
