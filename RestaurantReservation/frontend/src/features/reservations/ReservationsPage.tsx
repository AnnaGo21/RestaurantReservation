import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CalendarX, Plus, SearchX } from 'lucide-react'
import { DateNav } from '@/components/shared/DateNav'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import { FilterBar } from '@/components/shared/FilterBar'
import { ListSkeleton } from '@/components/shared/ListSkeleton'
import { PageHeader } from '@/components/shared/PageHeader'
import { SearchInput } from '@/components/shared/SearchInput'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { formatTime, todayIsoDate } from '@/lib/datetime'
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
      <PageHeader
        title={strings.nav.reservations}
        actions={
          <>
            <DateNav date={date} onChange={setDate} />
            <Button size="sm" onClick={() => setCreateOpen(true)}>
              <Plus className="size-4" />
              {strings.reservations.newButton}
            </Button>
          </>
        }
      />

      <FilterBar>
        <Input
          type="date"
          className="w-40"
          value={date}
          onChange={(event) => event.target.value && setDate(event.target.value)}
        />
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
        <SearchInput
          className="min-w-48 flex-1 sm:max-w-64"
          value={query}
          placeholder={strings.reservations.searchPlaceholder}
          onChange={(event) => setParam('q', event.target.value)}
          onClear={() => setParam('q', '')}
        />
      </FilterBar>

      {isPending && <ListSkeleton rows={6} />}

      {isError && <ErrorState message={error.message} onRetry={() => refetch()} />}

      {data && (
        <Card>
          <CardContent className="p-0">
            {data.length === 0 ? (
              <EmptyState icon={<CalendarX />} title={strings.reservations.empty} />
            ) : filtered.length === 0 ? (
              <EmptyState icon={<SearchX />} title={strings.reservations.noMatches} />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground">
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
                        className="cursor-pointer transition-colors hover:bg-slate-50"
                        onClick={() => setSelected(reservation)}
                      >
                        <td className="px-4 py-3 font-semibold text-foreground">
                          {formatTime(reservation.startTime)}
                          <span className="font-normal text-slate-400">
                            {' – '}
                            {formatTime(reservation.endTime)}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-medium text-foreground">
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
