import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/features/auth/use-auth'
import type { Role } from '@/types/user'

// UI-level gating only — the backend enforces the same matrix in SecurityConfig.
export function RequireRole({ roles }: { roles: Role[] }) {
  const { user } = useAuth()

  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/403" replace />
  }

  return <Outlet />
}
