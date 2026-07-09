import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'
import type { ReservationStatus } from '@/types/reservation'

const statusClasses: Record<ReservationStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  SEATED: 'bg-emerald-100 text-emerald-800',
  COMPLETED: 'bg-slate-100 text-slate-600',
  CANCELLED: 'bg-slate-200 text-slate-600',
  NO_SHOW: 'bg-red-100 text-red-800',
}

export function StatusBadge({ status }: { status: ReservationStatus }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        statusClasses[status],
      )}
    >
      {strings.status[status]}
    </span>
  )
}
