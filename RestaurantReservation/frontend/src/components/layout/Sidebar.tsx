import { NavLink } from 'react-router-dom'
import { useAuth } from '@/features/auth/use-auth'
import { ALL_ROLES, MANAGEMENT_ROLES } from '@/lib/roles'
import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'
import type { Role } from '@/types/user'

// Mirrors the backend SecurityConfig access matrix.
const navItems: { to: string; label: string; roles: Role[] }[] = [
  { to: '/dashboard', label: strings.nav.dashboard, roles: MANAGEMENT_ROLES },
  { to: '/reservations', label: strings.nav.reservations, roles: ALL_ROLES },
  { to: '/calendar', label: strings.nav.calendar, roles: ALL_ROLES },
  { to: '/tables', label: strings.nav.tables, roles: MANAGEMENT_ROLES },
  { to: '/guests', label: strings.nav.guests, roles: ALL_ROLES },
  { to: '/analytics', label: strings.nav.analytics, roles: MANAGEMENT_ROLES },
  { to: '/settings', label: strings.nav.settings, roles: ALL_ROLES },
]

interface SidebarProps {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const { user } = useAuth()

  if (!user) {
    return null
  }

  const visibleItems = navItems.filter((item) => item.roles.includes(user.role))

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-20 bg-slate-900/50 md:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-30 flex w-56 shrink-0 flex-col border-r border-slate-200 bg-white',
          'transition-transform md:static md:translate-x-0 md:transition-none',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 items-center border-b border-slate-200 px-5">
          <span className="text-base font-bold text-brand-700">{strings.appName}</span>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'block rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-brand-50 text-brand-700'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  )
}
