import { ApiError } from '../api/client.ts'
import { label } from '../format.ts'

export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) {
    return null
  }
  const apiError = error instanceof ApiError ? error : null
  return (
    <div className="error" role="alert">
      <p>{apiError?.message ?? 'Something went wrong. Please try again later.'}</p>
      {apiError && apiError.fieldErrors.length > 0 && (
        <ul>
          {apiError.fieldErrors.map((fieldError) => (
            <li key={`${fieldError.field}-${fieldError.message}`}>
              {label(fieldError.field)}: {fieldError.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
