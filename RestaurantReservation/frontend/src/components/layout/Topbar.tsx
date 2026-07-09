import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/features/auth/use-auth'
import { strings } from '@/lib/strings'

export function Topbar({ onMenuClick }: { onMenuClick: () => void }) {
  const { user, logout } = useAuth()

  if (!user) {
    return null
  }

  return (
    <header className="flex h-16 shrink-0 items-center gap-4 border-b border-slate-200 bg-white px-4 md:px-6">
      <button
        type="button"
        onClick={onMenuClick}
        aria-label={strings.a11y.openMenu}
        className="rounded-md p-2 text-slate-600 hover:bg-slate-100 md:hidden"
      >
        <svg className="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>
      <div className="ml-auto flex items-center gap-4">
        <div className="flex items-center gap-2">
          <span className="hidden text-sm font-medium text-slate-700 sm:inline">
            {user.fullName}
          </span>
          <Badge variant="brand">{user.role}</Badge>
        </div>
        <Button variant="secondary" size="sm" onClick={logout}>
          {strings.auth.logout}
        </Button>
      </div>
    </header>
  )
}
