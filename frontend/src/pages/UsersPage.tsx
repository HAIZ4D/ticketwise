import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/endpoints.ts'
import { ROLES } from '../api/types.ts'
import type { UserRole } from '../api/types.ts'
import { useCurrentUser } from '../auth/useAuth.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { count, formatDate, label } from '../format.ts'

export function UsersPage() {
  const currentUser = useCurrentUser()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const users = useQuery({
    queryKey: ['users', page],
    queryFn: () => api.listUsers(page),
    placeholderData: keepPreviousData,
  })
  const changeRole = useMutation({
    mutationFn: ({ id, role }: { id: number; role: UserRole }) => api.changeRole(id, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['agents'] })
    },
  })

  return (
    <>
      <title>Users · TicketWise</title>
      <h1>Users</h1>
      <ErrorMessage error={users.error ?? changeRole.error} />
      {users.isPending && <p className="status-text">Loading users…</p>}
      {users.data && (
        <>
          <div className="card table-card">
            <table>
              <thead>
                <tr>
                  <th scope="col">Email</th>
                  <th scope="col">Role</th>
                  <th scope="col" className="wide-only">
                    Joined
                  </th>
                </tr>
              </thead>
              <tbody>
                {users.data.content.map((user) => (
                  <tr key={user.id}>
                    <td className="title-cell">
                      {user.email} {user.id === currentUser.id && <span className="muted">(you)</span>}
                    </td>
                    <td>
                      <select
                        aria-label={`Role for ${user.email}`}
                        value={user.role}
                        disabled={user.id === currentUser.id || changeRole.isPending}
                        onChange={(e) => changeRole.mutate({ id: user.id, role: e.target.value as UserRole })}
                      >
                        {ROLES.map((role) => (
                          <option key={role} value={role}>
                            {label(role)}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="wide-only nowrap">{formatDate(user.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="pager" aria-label="Pages">
            <button type="button" className="button-quiet" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span className="muted">
              Page {page + 1} of {Math.max(users.data.totalPages, 1)} · {count(users.data.totalElements, 'user')}
            </span>
            <button
              type="button"
              className="button-quiet"
              disabled={page + 1 >= users.data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </>
  )
}
