import { StatusBadge } from '@/components/shared/StatusBadge'
import { Dialog } from '@/components/ui/dialog'
import { formatDateLabel, formatTime } from '@/lib/datetime'
import { strings } from '@/lib/strings'
import type { Reservation } from '@/types/reservation'
import { ReservationActions } from './ReservationActions'

interface ReservationDetailProps {
  reservation: Reservation | null
  tableLabel: string | null
  onClose: () => void
  onUpdated: (reservation: Reservation) => void
  onMove: () => void
}

export function ReservationDetail({
  reservation,
  tableLabel,
  onClose,
  onUpdated,
  onMove,
}: ReservationDetailProps) {
  const labels = strings.reservations

  if (!reservation) {
    return null
  }

  const rows: { label: string; value: React.ReactNode }[] = [
    { label: labels.detailDate, value: formatDateLabel(reservation.startTime.slice(0, 10)) },
    {
      label: labels.detailTime,
      value: `${formatTime(reservation.startTime)} – ${formatTime(reservation.endTime)}`,
    },
    { label: labels.detailGuest, value: reservation.guestName },
    {
      label: labels.detailPhone,
      value: reservation.guestPhone.startsWith('walkin-') ? labels.none : reservation.guestPhone,
    },
    { label: labels.detailParty, value: reservation.partySize },
    { label: labels.detailTable, value: tableLabel ?? `#${reservation.tableId}` },
    { label: labels.detailStatus, value: <StatusBadge status={reservation.status} /> },
    { label: labels.detailNotes, value: reservation.notes || labels.none },
    {
      label: labels.detailCheckedIn,
      value: reservation.checkedInAt
        ? `${formatDateLabel(reservation.checkedInAt.slice(0, 10))}, ${formatTime(reservation.checkedInAt)}`
        : labels.none,
    },
    { label: labels.detailReminder, value: reservation.reminderSent ? labels.yes : labels.no },
  ]

  return (
    <Dialog open onClose={onClose} title={labels.detailTitle}>
      <dl className="divide-y divide-slate-100">
        {rows.map((row) => (
          <div key={row.label} className="flex items-center justify-between gap-4 py-2.5">
            <dt className="text-sm text-slate-500">{row.label}</dt>
            <dd className="text-right text-sm font-medium text-slate-900">{row.value}</dd>
          </div>
        ))}
      </dl>
      <ReservationActions reservation={reservation} onUpdated={onUpdated} onMove={onMove} />
    </Dialog>
  )
}
