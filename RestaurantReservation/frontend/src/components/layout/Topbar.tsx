import { ChevronDown, LogOut, Menu } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dropdown, DropdownItem, DropdownSeparator } from '@/components/ui/dropdown-menu'
import { useAuth } from '@/features/auth/use-auth'
import { strings } from '@/lib/strings'

export function Topbar({ onMenuClick }: { onMenuClick: () => void }) {
  const { user, logout } = useAuth()

  if (!user) {
    return null
  }

  return (
    <header className="flex h-16 shrink-0 items-center gap-3 border-b border-border bg-card px-4 md:px-6">
      <Button
        variant="ghost"
        size="icon-sm"
        onClick={onMenuClick}
        aria-label={strings.a11y.openMenu}
        className="lg:hidden"
      >
        <Menu className="size-5" />
      </Button>
      <div className="ml-auto">
        <Dropdown
          ariaLabel={strings.a11y.userMenu}
          triggerClassName="p-1 pr-1.5 hover:bg-muted"
          trigger={
            <>
              <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-brand-100 text-xs font-semibold text-brand-700">
                {initialsOf(user.fullName)}
              </span>
              <span className="hidden max-w-40 truncate text-sm font-medium text-foreground sm:block">
                {user.fullName}
              </span>
              <ChevronDown className="size-4 text-slate-400" />
            </>
          }
        >
          <div className="px-2.5 py-2">
            <p className="truncate text-sm font-medium text-foreground">{user.fullName}</p>
            <p className="truncate text-xs text-muted-foreground">{user.email}</p>
            <Badge variant="brand" className="mt-1.5">
              {user.role}
            </Badge>
          </div>
          <DropdownSeparator />
          <DropdownItem onClick={logout}>
            <LogOut />
            {strings.auth.logout}
          </DropdownItem>
        </Dropdown>
      </div>
    </header>
  )
}

function initialsOf(fullName: string): string {
  return fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join('')
}
