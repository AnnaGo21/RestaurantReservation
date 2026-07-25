import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'
import type { ReservationStatus } from '@/types/reservation'

const statusClasses: Record<ReservationStatus, { badge: string; dot: string }> = {
  PENDING: { badge: 'border-amber-200 bg-amber-50 text-amber-700', dot: 'bg-amber-500' },
  CONFIRMED: { badge: 'border-blue-200 bg-blue-50 text-blue-700', dot: 'bg-blue-500' },
  SEATED: { badge: 'border-emerald-200 bg-emerald-50 text-emerald-700', dot: 'bg-emerald-500' },
  COMPLETED: { badge: 'border-slate-200 bg-slate-50 text-slate-600', dot: 'bg-slate-400' },
  CANCELLED: { badge: 'border-slate-200 bg-slate-100 text-slate-500', dot: 'bg-slate-400' },
  NO_SHOW: { badge: 'border-red-200 bg-red-50 text-red-700', dot: 'bg-red-500' },
}

export function StatusBadge({ status }: { status: ReservationStatus }) {
  const classes = statusClasses[status]

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-0.5 text-xs font-medium',
        classes.badge,
      )}
    >
      <span className={cn('size-1.5 rounded-full', classes.dot)} aria-hidden="true" />
      {strings.status[status]}
    </span>
  )
}
