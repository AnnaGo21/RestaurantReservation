import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

type BadgeVariant = 'neutral' | 'brand'

const variantClasses: Record<BadgeVariant, string> = {
  neutral: 'border-slate-200 bg-muted text-slate-700',
  brand: 'border-brand-200 bg-brand-50 text-brand-700',
}

interface BadgeProps extends ComponentProps<'span'> {
  variant?: BadgeVariant
}

export function Badge({ variant = 'neutral', className, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium',
        variantClasses[variant],
        className,
      )}
      {...props}
    />
  )
}
