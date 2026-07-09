import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth/use-auth'
import { homePathFor } from '@/lib/roles'

// Rendered under RequireAuth, so a user is always present here.
export function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace />
}
