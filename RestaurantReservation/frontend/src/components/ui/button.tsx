import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md' | 'icon' | 'icon-sm'

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'bg-primary text-primary-foreground shadow-xs hover:bg-brand-700 active:bg-brand-800 focus-visible:ring-ring/40',
  secondary:
    'border border-input bg-card text-slate-700 shadow-xs hover:bg-slate-50 hover:text-foreground active:bg-muted focus-visible:ring-slate-400/40',
  ghost:
    'text-slate-600 hover:bg-muted hover:text-foreground active:bg-slate-200/60 focus-visible:ring-slate-400/40',
  danger:
    'bg-destructive text-destructive-foreground shadow-xs hover:bg-red-700 active:bg-red-800 focus-visible:ring-destructive/40',
}

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-10 px-4 text-sm',
  icon: 'size-10',
  'icon-sm': 'size-8',
}

interface ButtonProps extends ComponentProps<'button'> {
  variant?: ButtonVariant
  size?: ButtonSize
}

export function Button({
  variant = 'primary',
  size = 'md',
  className,
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        'inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-md font-medium transition-colors',
        'outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-background',
        'disabled:pointer-events-none disabled:opacity-50',
        '[&_svg]:pointer-events-none [&_svg]:shrink-0',
        variantClasses[variant],
        sizeClasses[size],
        className,
      )}
      {...props}
    />
  )
}
