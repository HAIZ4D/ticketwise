import { useMutation } from '@tanstack/react-query'
import { api } from '../../api/endpoints.ts'
import type { Suggestion } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { SimilarTicketList } from './SimilarTicketList.tsx'

export function SuggestionPanel({ ticketId }: { ticketId: string }) {
  const suggest = useMutation({ mutationFn: () => api.suggestFix(ticketId) })

  return (
    <section aria-label="Suggested fix">
      <h2>Suggested fix</h2>
      <p className="muted">Based on how similar tickets were solved.</p>
      <div>
        <button type="button" className="button-quiet" disabled={suggest.isPending} onClick={() => suggest.mutate()}>
          {suggest.isPending ? 'Looking…' : 'Suggest a fix'}
        </button>
      </div>
      <ErrorMessage error={suggest.error} />
      {suggest.data && <SuggestionResult result={suggest.data} />}
    </section>
  )
}

function SuggestionResult({ result }: { result: Suggestion }) {
  if (result.sources.length === 0) {
    return <p className="muted">No similar solved tickets yet.</p>
  }
  return (
    <>
      {result.suggestion && (
        <>
          <p className="description">{result.suggestion}</p>
          <p className="note">AI suggestion. Check it before you use it.</p>
        </>
      )}
      <h3>{result.suggestion ? 'Based on' : 'Similar solved tickets'}</h3>
      <SimilarTicketList tickets={result.sources} />
    </>
  )
}
