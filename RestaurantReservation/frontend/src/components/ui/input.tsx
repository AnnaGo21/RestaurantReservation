import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

export function Input({ className, ...props }: ComponentProps<'input'>) {
  return (
    <input
      className={cn(
        'h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-900',
        'placeholder:text-slate-400',
        'focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500/30',
        'disabled:cursor-not-allowed disabled:bg-slate-100',
        className,
      )}
      {...props}
    />
  )
}
