import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/endpoints.ts'
import { SimilarTicketList } from './SimilarTicketList.tsx'

export function DuplicatesPanel({ ticketId }: { ticketId: string }) {
  const duplicates = useQuery({ queryKey: ['duplicates', ticketId], queryFn: () => api.listDuplicates(ticketId) })

  if (!duplicates.data?.length) {
    return null
  }
  return (
    <section className="card" aria-label="Possible duplicates">
      <h2>Possible duplicates</h2>
      <SimilarTicketList tickets={duplicates.data} />
    </section>
  )
}
