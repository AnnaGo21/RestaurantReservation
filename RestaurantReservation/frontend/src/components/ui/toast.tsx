import { useState, useCallback } from 'react'
import { X } from 'lucide-react'
import { ToastContext, type ToastOptions } from './toast-context'
import { cn } from '@/lib/utils'

interface ToastItem extends ToastOptions {
  id: number
}

let nextId = 0

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const toast = useCallback((options: ToastOptions) => {
    const id = ++nextId
    setToasts((prev) => [...prev, { ...options, id }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 3500)
  }, [])

  const dismiss = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            style={{ animation: 'var(--animate-toast-in)' }}
            className={cn(
              'flex min-w-64 items-center gap-3 rounded-lg border bg-popover px-4 py-3 text-sm shadow-lg',
              t.variant === 'error'
                ? 'border-red-200 bg-red-50 text-red-800'
                : 'border-border text-foreground',
            )}
          >
            <span className="flex-1 font-medium">{t.title}</span>
            <button
              type="button"
              onClick={() => dismiss(t.id)}
              className="text-muted-foreground hover:text-foreground"
              aria-label="Dismiss"
            >
              <X className="size-3.5" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
