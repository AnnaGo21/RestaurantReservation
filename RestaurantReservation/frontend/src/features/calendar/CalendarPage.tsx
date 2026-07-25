import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CalendarDays } from 'lucide-react'
import { DateNav } from '@/components/shared/DateNav'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import { ListSkeleton } from '@/components/shared/ListSkeleton'
import { PageHeader } from '@/components/shared/PageHeader'
import { Card, CardContent } from '@/components/ui/card'
import { MoveReservationModal } from '@/features/reservations/MoveReservationModal'
import { ReservationDetail } from '@/features/reservations/ReservationDetail'
import { useDailyReservations } from '@/features/reservations/use-reservations'
import { useTables } from '@/features/tables/use-tables'
import { formatTime, todayIsoDate } from '@/lib/datetime'
import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'
import type { Reservation, ReservationStatus } from '@/types/reservation'

const HOUR_WIDTH = 80
const LABEL_WIDTH = 112

const blockClasses: Record<ReservationStatus, string> = {
  PENDING: 'border-amber-300 bg-amber-100 text-amber-900 hover:bg-amber-200',
  CONFIRMED: 'border-blue-300 bg-blue-100 text-blue-900 hover:bg-blue-200',
  SEATED: 'border-emerald-300 bg-emerald-100 text-emerald-900 hover:bg-emerald-200',
  COMPLETED: 'border-slate-300 bg-slate-100 text-slate-600 hover:bg-slate-200',
  CANCELLED: 'border-slate-300 bg-slate-100 text-slate-500',
  NO_SHOW: 'border-red-300 bg-red-100 text-red-900',
}

export function CalendarPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const date = searchParams.get('date') ?? todayIsoDate()
  const [selected, setSelected] = useState<Reservation | null>(null)
  const [moving, setMoving] = useState<Reservation | null>(null)

  const { data, isPending, isError, error, refetch } = useDailyReservations(date)
  const tablesQuery = useTables()
  const tableLabelById = new Map((tablesQuery.data ?? []).map((table) => [table.id, table.label]))

  const setDate = (next: string) => setSearchParams({ date: next })

  // Cancelled and no-show reservations do not occupy tables, so they would
  // only overlap real bookings on the grid.
  const visible = (data ?? []).filter(
    (reservation) => reservation.status !== 'CANCELLED' && reservation.status !== 'NO_SHOW',
  )

  const rows: { id: number; label: string }[] = tablesQuery.data?.length
    ? tablesQuery.data.map((table) => ({ id: table.id, label: table.label }))
    : [...new Set(visible.map((reservation) => reservation.tableId))]
        .sort((a, b) => a - b)
        .map((id) => ({ id, label: `#${id}` }))

  let startHour = 10
  let endHour = 24
  for (const reservation of visible) {
    startHour = Math.min(startHour, Math.floor(minutesOf(reservation.startTime) / 60))
    endHour = Math.max(endHour, Math.ceil(endMinutes(reservation, date) / 60))
  }
  const hours = Array.from({ length: endHour - startHour }, (_, i) => startHour + i)

  return (
    <div>
      <PageHeader
        title={strings.nav.calendar}
        actions={<DateNav date={date} onChange={setDate} />}
      />

      {isPending && <ListSkeleton rows={6} />}

      {isError && <ErrorState message={error.message} onRetry={() => refetch()} />}

      {data && (
        <Card>
          <CardContent className="p-0">
            {rows.length === 0 ? (
              <EmptyState icon={<CalendarDays />} title={strings.calendar.empty} />
            ) : (
              <div className="overflow-x-auto">
                <div style={{ minWidth: LABEL_WIDTH + hours.length * HOUR_WIDTH }}>
                  <div className="flex border-b border-slate-200">
                    <div
                      className="sticky left-0 z-10 shrink-0 bg-white"
                      style={{ width: LABEL_WIDTH }}
                    />
                    {hours.map((hour) => (
                      <div
                        key={hour}
                        className="shrink-0 border-l border-slate-100 px-1.5 py-2 text-xs font-medium text-slate-500"
                        style={{ width: HOUR_WIDTH }}
                      >
                        {String(hour).padStart(2, '0')}:00
                      </div>
                    ))}
                  </div>

                  {rows.map((row) => (
                    <div key={row.id} className="flex border-b border-slate-100">
                      <div
                        className="sticky left-0 z-10 flex shrink-0 items-center border-r border-slate-200 bg-white px-3 text-sm font-medium text-slate-700"
                        style={{ width: LABEL_WIDTH }}
                      >
                        <span className="truncate">{row.label}</span>
                      </div>
                      <div
                        className="relative h-14 shrink-0"
                        style={{ width: hours.length * HOUR_WIDTH }}
                      >
                        {hours.map((hour) => (
                          <div
                            key={hour}
                            className="absolute inset-y-0 border-l border-slate-100"
                            style={{ left: (hour - startHour) * HOUR_WIDTH }}
                          />
                        ))}
                        {visible
                          .filter((reservation) => reservation.tableId === row.id)
                          .map((reservation) => {
                            const start = minutesOf(reservation.startTime)
                            const end = endMinutes(reservation, date)
                            const left = ((start - startHour * 60) / 60) * HOUR_WIDTH
                            const width = Math.max(((end - start) / 60) * HOUR_WIDTH, 32)
                            return (
                              <button
                                key={reservation.id}
                                type="button"
                                onClick={() => setSelected(reservation)}
                                className={cn(
                                  'absolute inset-y-1.5 overflow-hidden rounded-md border px-1.5 text-left text-xs transition-colors',
                                  blockClasses[reservation.status],
                                )}
                                style={{ left, width }}
                              >
                                <span className="block truncate font-semibold">
                                  {formatTime(reservation.startTime)} {reservation.guestName}
                                </span>
                                <span className="block truncate">
                                  {reservation.partySize} {strings.dashboard.guestsSuffix}
                                </span>
                              </button>
                            )
                          })}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <ReservationDetail
        reservation={selected}
        tableLabel={selected ? (tableLabelById.get(selected.tableId) ?? null) : null}
        onClose={() => setSelected(null)}
        onUpdated={setSelected}
        onMove={() => {
          setMoving(selected)
          setSelected(null)
        }}
      />

      {moving && <MoveReservationModal reservation={moving} onClose={() => setMoving(null)} />}
    </div>
  )
}

function minutesOf(isoDateTime: string): number {
  return Number(isoDateTime.slice(11, 13)) * 60 + Number(isoDateTime.slice(14, 16))
}

// A reservation ending after midnight is clamped to the end of the viewed day.
function endMinutes(reservation: Reservation, date: string): number {
  if (reservation.endTime.slice(0, 10) !== date) {
    return 24 * 60
  }
  return minutesOf(reservation.endTime)
}
