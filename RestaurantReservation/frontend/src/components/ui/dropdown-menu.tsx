import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ComponentProps,
  type ReactNode,
} from 'react'
import { cn } from '@/lib/utils'

const DropdownContext = createContext<{ close: () => void } | null>(null)

interface DropdownProps {
  trigger: ReactNode
  triggerClassName?: string
  ariaLabel?: string
  align?: 'start' | 'end'
  children: ReactNode
}

export function Dropdown({
  trigger,
  triggerClassName,
  ariaLabel,
  align = 'end',
  children,
}: DropdownProps) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    const onPointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={ariaLabel}
        onClick={() => setOpen((prev) => !prev)}
        className={cn(
          'flex items-center gap-2 rounded-md outline-none transition-colors',
          'focus-visible:ring-2 focus-visible:ring-ring/40 focus-visible:ring-offset-2 focus-visible:ring-offset-background',
          triggerClassName,
        )}
      >
        {trigger}
      </button>
      {open && (
        <DropdownContext.Provider value={{ close: () => setOpen(false) }}>
          <div
            role="menu"
            className={cn(
              'animate-dialog-in absolute top-full z-50 mt-1.5 min-w-56 rounded-lg border border-border bg-card p-1 shadow-md',
              align === 'end' ? 'right-0' : 'left-0',
            )}
          >
            {children}
          </div>
        </DropdownContext.Provider>
      )}
    </div>
  )
}

export function DropdownItem({ className, onClick, ...props }: ComponentProps<'button'>) {
  const context = useContext(DropdownContext)

  return (
    <button
      type="button"
      role="menuitem"
      onClick={(event) => {
        onClick?.(event)
        context?.close()
      }}
      className={cn(
        'flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left text-sm text-slate-700 transition-colors',
        'hover:bg-muted hover:text-foreground focus-visible:bg-muted focus-visible:outline-none',
        '[&_svg]:size-4 [&_svg]:shrink-0 [&_svg]:text-slate-400',
        className,
      )}
      {...props}
    />
  )
}

export function DropdownSeparator() {
  return <div role="separator" className="my-1 h-px bg-border" />
}
