import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { FullScreenSpinner } from '@/components/ui/spinner'
import { useAuth } from '@/features/auth/use-auth'

export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <FullScreenSpinner />
  }

  if (status === 'unauthenticated') {
    return (
      <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
    )
  }

  return <Outlet />
}
