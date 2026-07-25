import { Armchair, CalendarDays, CalendarX, Clock, UserX } from 'lucide-react'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import { KpiCard } from '@/components/shared/KpiCard'
import { ListSkeleton } from '@/components/shared/ListSkeleton'
import { PageHeader } from '@/components/shared/PageHeader'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
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
      <PageHeader title={strings.nav.dashboard} />

      {isPending && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            {Array.from({ length: 4 }, (_, index) => (
              <Skeleton key={index} className="h-24" />
            ))}
          </div>
          <ListSkeleton />
        </div>
      )}

      {isError && <ErrorState message={error.message} onRetry={() => refetch()} />}

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

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      <KpiCard
        label={strings.dashboard.statToday}
        value={String(data.totalReservationsToday)}
        icon={<CalendarDays />}
      />
      <KpiCard
        label={strings.dashboard.statUpcoming}
        value={String(data.upcomingReservations.length)}
        icon={<Clock />}
      />
      <KpiCard
        label={strings.dashboard.statOccupied}
        value={`${data.occupiedTables.length} / ${totalTables}`}
        icon={<Armchair />}
      />
      <KpiCard
        label={strings.dashboard.statNoShow}
        value={`${data.noShowPercentage.toFixed(1)}%`}
        icon={<UserX />}
      />
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
      <CardContent className={cn(!hasTables && 'p-0')}>
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
          <EmptyState icon={<Armchair />} title={strings.dashboard.emptyTables} className="py-8" />
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
          : 'border-input bg-card text-slate-700',
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
          <EmptyState icon={<CalendarX />} title={emptyText} className="py-8" />
        ) : (
          <ul className="divide-y divide-slate-100">
            {reservations.map((reservation) => (
              <li key={reservation.id} className="flex items-center gap-3 px-6 py-3">
                <span className="w-12 shrink-0 text-sm font-semibold text-foreground">
                  {formatTime(reservation.startTime)}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">
                    {reservation.guestName}
                  </p>
                  <p className="text-xs text-muted-foreground">
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
