import type { Role } from '@/types/user'

export const ALL_ROLES: Role[] = ['OWNER', 'MANAGER', 'STAFF']
export const MANAGEMENT_ROLES: Role[] = ['OWNER', 'MANAGER']

// STAFF cannot access /api/dashboard (see SecurityConfig), so their home is the reservations page.
export function homePathFor(role: Role): string {
  return role === 'STAFF' ? '/reservations' : '/dashboard'
}
