import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Spinner } from '@/components/ui/spinner'
import { formatDateLabel, formatTime, shiftIsoDate, todayIsoDate } from '@/lib/datetime'
import { strings } from '@/lib/strings'
import { useTables } from '@/features/tables/use-tables'
import { ALL_RESERVATION_STATUSES, type Reservation } from '@/types/reservation'
import { CreateReservationModal } from './CreateReservationModal'
import { MoveReservationModal } from './MoveReservationModal'
import { ReservationDetail } from './ReservationDetail'
import { useDailyReservations } from './use-reservations'

export function ReservationsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const date = searchParams.get('date') ?? todayIsoDate()
  const [selected, setSelected] = useState<Reservation | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [moving, setMoving] = useState<Reservation | null>(null)

  const rawStatus = searchParams.get('status')
  const statusFilter = ALL_RESERVATION_STATUSES.find((status) => status === rawStatus) ?? null
  const query = searchParams.get('q') ?? ''

  const { data, isPending, isError, error, refetch } = useDailyReservations(date)
  const tablesQuery = useTables()
  const tableLabelById = new Map((tablesQuery.data ?? []).map((table) => [table.id, table.label]))

  const setParam = (key: 'date' | 'status' | 'q', value: string) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        if (value) {
          next.set(key, value)
        } else {
          next.delete(key)
        }
        return next
      },
      // Typing in the search box should not spam browser history.
      { replace: key === 'q' },
    )
  }
  const setDate = (next: string) => setParam('date', next)

  const normalizedQuery = query.trim().toLowerCase()
  const filtered = (data ?? []).filter((reservation) => {
    if (statusFilter && reservation.status !== statusFilter) {
      return false
    }
    if (!normalizedQuery) {
      return true
    }
    return (
      reservation.guestName.toLowerCase().includes(normalizedQuery) ||
      reservation.guestPhone.toLowerCase().includes(normalizedQuery)
    )
  })

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-slate-900">{strings.nav.reservations}</h1>
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            aria-label={strings.common.previousDay}
            onClick={() => setDate(shiftIsoDate(date, -1))}
          >
            ‹
          </Button>
          <span className="min-w-40 text-center text-sm font-medium text-slate-700">
            {formatDateLabel(date)}
          </span>
          <Button
            variant="secondary"
            size="sm"
            aria-label={strings.common.nextDay}
            onClick={() => setDate(shiftIsoDate(date, 1))}
          >
            ›
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setDate(todayIsoDate())}>
            {strings.common.today}
          </Button>
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            {strings.reservations.newButton}
          </Button>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="w-40">
          <Input
            type="date"
            value={date}
            onChange={(event) => event.target.value && setDate(event.target.value)}
          />
        </div>
        <Select
          value={statusFilter ?? ''}
          onChange={(event) => setParam('status', event.target.value)}
        >
          <option value="">{strings.reservations.statusAll}</option>
          {ALL_RESERVATION_STATUSES.map((status) => (
            <option key={status} value={status}>
              {strings.status[status]}
            </option>
          ))}
        </Select>
        <div className="min-w-48 flex-1 sm:max-w-64">
          <Input
            value={query}
            placeholder={strings.reservations.searchPlaceholder}
            onChange={(event) => setParam('q', event.target.value)}
          />
        </div>
      </div>

      {isPending && (
        <div className="flex justify-center py-16">
          <Spinner className="size-8" />
        </div>
      )}

      {isError && (
        <Card>
          <CardContent className="flex flex-col items-center gap-3 py-10">
            <p className="text-sm text-red-600">{error.message}</p>
            <Button variant="secondary" size="sm" onClick={() => refetch()}>
              {strings.common.retry}
            </Button>
          </CardContent>
        </Card>
      )}

      {data && (
        <Card>
          <CardContent className="p-0">
            {data.length === 0 ? (
              <p className="px-6 py-10 text-center text-sm text-slate-500">
                {strings.reservations.empty}
              </p>
            ) : filtered.length === 0 ? (
              <p className="px-6 py-10 text-center text-sm text-slate-500">
                {strings.reservations.noMatches}
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                      <th className="px-4 py-3">{strings.reservations.colTime}</th>
                      <th className="px-4 py-3">{strings.reservations.colGuest}</th>
                      <th className="px-4 py-3">{strings.reservations.colPhone}</th>
                      <th className="px-4 py-3">{strings.reservations.colParty}</th>
                      <th className="px-4 py-3">{strings.reservations.colTable}</th>
                      <th className="px-4 py-3">{strings.reservations.colStatus}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {sortByStartTime(filtered).map((reservation) => (
                      <tr
                        key={reservation.id}
                        className="cursor-pointer hover:bg-slate-50"
                        onClick={() => setSelected(reservation)}
                      >
                        <td className="px-4 py-3 font-semibold text-slate-900">
                          {formatTime(reservation.startTime)}
                          <span className="font-normal text-slate-400">
                            {' – '}
                            {formatTime(reservation.endTime)}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-medium text-slate-900">
                          {reservation.guestName}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {displayPhone(reservation.guestPhone)}
                        </td>
                        <td className="px-4 py-3 text-slate-600">{reservation.partySize}</td>
                        <td className="px-4 py-3 text-slate-600">
                          {tableLabelById.get(reservation.tableId) ?? `#${reservation.tableId}`}
                        </td>
                        <td className="px-4 py-3">
                          <StatusBadge status={reservation.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
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

      {createOpen && (
        <CreateReservationModal defaultDate={date} onClose={() => setCreateOpen(false)} />
      )}

      {moving && <MoveReservationModal reservation={moving} onClose={() => setMoving(null)} />}
    </div>
  )
}

function sortByStartTime(reservations: Reservation[]): Reservation[] {
  return [...reservations].sort((a, b) => a.startTime.localeCompare(b.startTime))
}

// Anonymous walk-ins get a synthetic "walkin-{uuid}" phone on the backend.
function displayPhone(phone: string): string {
  return phone.startsWith('walkin-') ? '—' : phone
}
