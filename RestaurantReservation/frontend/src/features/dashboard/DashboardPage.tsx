import { StatusBadge } from '@/components/shared/StatusBadge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Spinner } from '@/components/ui/spinner'
import { formatTime } from '@/lib/datetime'
import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'
import type { DashboardData } from '@/types/dashboard'
import type { Reservation } from '@/types/reservation'
import type { RestaurantTable } from '@/types/table'
import { useDashboard } from './use-dashboard'

export function DashboardPage() {
  const { data, isPending, isError, error, refetch } = useDashboard()

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-slate-900">{strings.nav.dashboard}</h1>

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
              {strings.dashboard.retry}
            </Button>
          </CardContent>
        </Card>
      )}

      {data && (
        <div className="space-y-4">
          <StatCards data={data} />
          <TableOccupancy occupied={data.occupiedTables} free={data.freeTables} />
          <div className="grid gap-4 lg:grid-cols-2">
            <ReservationList
              title={strings.dashboard.todayTitle}
              reservations={data.todayReservations}
              emptyText={strings.dashboard.emptyToday}
              tables={[...data.occupiedTables, ...data.freeTables]}
            />
            <ReservationList
              title={strings.dashboard.upcomingTitle}
              reservations={data.upcomingReservations}
              emptyText={strings.dashboard.emptyUpcoming}
              tables={[...data.occupiedTables, ...data.freeTables]}
            />
          </div>
        </div>
      )}
    </div>
  )
}

function StatCards({ data }: { data: DashboardData }) {
  const totalTables = data.occupiedTables.length + data.freeTables.length
  const stats = [
    { label: strings.dashboard.statToday, value: String(data.totalReservationsToday) },
    { label: strings.dashboard.statUpcoming, value: String(data.upcomingReservations.length) },
    {
      label: strings.dashboard.statOccupied,
      value: `${data.occupiedTables.length} / ${totalTables}`,
    },
    { label: strings.dashboard.statNoShow, value: `${data.noShowPercentage.toFixed(1)}%` },
  ]

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {stats.map((stat) => (
        <Card key={stat.label}>
          <CardContent className="py-4">
            <p className="text-sm text-slate-500">{stat.label}</p>
            <p className="mt-1 text-2xl font-bold text-slate-900">{stat.value}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

function TableOccupancy({
  occupied,
  free,
}: {
  occupied: RestaurantTable[]
  free: RestaurantTable[]
}) {
  const hasTables = occupied.length > 0 || free.length > 0

  return (
    <Card>
      <CardHeader>
        <CardTitle>{strings.dashboard.occupancyTitle}</CardTitle>
      </CardHeader>
      <CardContent>
        {hasTables ? (
          <div className="flex flex-wrap gap-2">
            {occupied.map((table) => (
              <TableChip key={table.id} table={table} occupied />
            ))}
            {free.map((table) => (
              <TableChip key={table.id} table={table} />
            ))}
          </div>
        ) : (
          <p className="text-sm text-slate-500">{strings.dashboard.emptyTables}</p>
        )}
      </CardContent>
    </Card>
  )
}

function TableChip({ table, occupied = false }: { table: RestaurantTable; occupied?: boolean }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm font-medium',
        occupied
          ? 'border-brand-600 bg-brand-600 text-white'
          : 'border-slate-300 bg-white text-slate-700',
      )}
    >
      {table.label}
      <span className={cn('text-xs', occupied ? 'text-brand-100' : 'text-slate-400')}>
        {table.capacity} {strings.dashboard.seatsSuffix}
      </span>
    </span>
  )
}

function ReservationList({
  title,
  reservations,
  emptyText,
  tables,
}: {
  title: string
  reservations: Reservation[]
  emptyText: string
  tables: RestaurantTable[]
}) {
  const tableLabelById = new Map(tables.map((table) => [table.id, table.label]))

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        {reservations.length === 0 ? (
          <p className="px-6 py-8 text-center text-sm text-slate-500">{emptyText}</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {reservations.map((reservation) => (
              <li key={reservation.id} className="flex items-center gap-3 px-6 py-3">
                <span className="w-12 shrink-0 text-sm font-semibold text-slate-900">
                  {formatTime(reservation.startTime)}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-slate-900">
                    {reservation.guestName}
                  </p>
                  <p className="text-xs text-slate-500">
                    {tableLabelById.get(reservation.tableId) ?? `#${reservation.tableId}`}
                    {' · '}
                    {reservation.partySize} {strings.dashboard.guestsSuffix}
                  </p>
                </div>
                <StatusBadge status={reservation.status} />
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
