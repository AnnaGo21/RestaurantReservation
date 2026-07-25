import { NavLink } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Armchair,
  BarChart3,
  CalendarDays,
  ClipboardList,
  LayoutDashboard,
  Settings,
  Users,
  UtensilsCrossed,
  type LucideIcon,
} from 'lucide-react'
import { getRestaurants } from '@/api/restaurants'
import { useAuth } from '@/features/auth/use-auth'
import { ALL_ROLES, MANAGEMENT_ROLES } from '@/lib/roles'
import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'
import type { Role } from '@/types/user'

// Mirrors the backend SecurityConfig access matrix.
const navItems: { to: string; label: string; icon: LucideIcon; roles: Role[] }[] = [
  { to: '/dashboard', label: strings.nav.dashboard, icon: LayoutDashboard, roles: MANAGEMENT_ROLES },
  { to: '/reservations', label: strings.nav.reservations, icon: ClipboardList, roles: ALL_ROLES },
  { to: '/calendar', label: strings.nav.calendar, icon: CalendarDays, roles: ALL_ROLES },
  { to: '/tables', label: strings.nav.tables, icon: Armchair, roles: MANAGEMENT_ROLES },
  { to: '/guests', label: strings.nav.guests, icon: Users, roles: ALL_ROLES },
  { to: '/analytics', label: strings.nav.analytics, icon: BarChart3, roles: MANAGEMENT_ROLES },
]

interface SidebarProps {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const { user } = useAuth()

  const { data: restaurants } = useQuery({
    queryKey: ['restaurants'],
    queryFn: getRestaurants,
    staleTime: Infinity,
  })

  if (!user) {
    return null
  }

  const restaurantName = restaurants?.find(
    (restaurant) => restaurant.id === user.restaurantId,
  )?.name
  const visibleItems = navItems.filter((item) => item.roles.includes(user.role))

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-20 bg-slate-950/40 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-30 flex w-60 shrink-0 flex-col border-r border-border bg-card',
          'transition-transform lg:static lg:translate-x-0 lg:transition-none',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border px-4">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <UtensilsCrossed className="size-4" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-foreground">
              {restaurantName ?? strings.appName}
            </p>
            {restaurantName && (
              <p className="truncate text-xs text-muted-foreground">{strings.appName}</p>
            )}
          </div>
        </div>
        <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
          {visibleItems.map((item) => (
            <SidebarLink key={item.to} {...item} onClick={onClose} />
          ))}
        </nav>
        <div className="border-t border-border p-3">
          <SidebarLink
            to="/settings"
            label={strings.nav.settings}
            icon={Settings}
            onClick={onClose}
          />
        </div>
      </aside>
    </>
  )
}

function SidebarLink({
  to,
  label,
  icon: Icon,
  onClick,
}: {
  to: string
  label: string
  icon: LucideIcon
  onClick: () => void
}) {
  return (
    <NavLink
      to={to}
      onClick={onClick}
      className={({ isActive }) =>
        cn(
          'group flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
          isActive
            ? 'bg-accent text-brand-700'
            : 'text-slate-600 hover:bg-muted hover:text-foreground',
        )
      }
    >
      {({ isActive }) => (
        <>
          <Icon
            className={cn(
              'size-4 shrink-0',
              isActive ? 'text-brand-600' : 'text-slate-400 group-hover:text-slate-500',
            )}
          />
          {label}
        </>
      )}
    </NavLink>
  )
}
