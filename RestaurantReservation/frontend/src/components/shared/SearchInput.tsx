import type { ComponentProps } from 'react'
import { Search, X } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'

interface SearchInputProps extends ComponentProps<'input'> {
  onClear?: () => void
}

export function SearchInput({ className, value, onClear, ...props }: SearchInputProps) {
  const showClear = Boolean(onClear) && typeof value === 'string' && value.length > 0

  return (
    <div className={cn('relative', className)}>
      <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
      <Input type="search" value={value} className={cn('pl-9', showClear && 'pr-9')} {...props} />
      {showClear && (
        <button
          type="button"
          aria-label={strings.a11y.clearSearch}
          onClick={onClear}
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-slate-400 transition-colors hover:bg-muted hover:text-slate-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
        >
          <X className="size-4" />
        </button>
      )}
    </div>
  )
}
