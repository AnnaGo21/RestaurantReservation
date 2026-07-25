import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

export function FilterBar({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <div className={cn('mb-4 flex flex-wrap items-center gap-2', className)}>{children}</div>
  )
}
