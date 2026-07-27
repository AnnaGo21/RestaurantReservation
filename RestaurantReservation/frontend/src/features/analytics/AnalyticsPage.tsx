import { useMemo, useState, type ReactNode } from 'react'
import { BarChart3, CalendarCheck, CalendarX, CheckCircle2, Clock, Users, UserX } from 'lucide-react'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import { KpiCard } from '@/components/shared/KpiCard'
import { PageHeader } from '@/components/shared/PageHeader'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'
import type { AnalyticsResponse } from '@/types/analytics'
import { useAnalytics } from './use-analytics'

type TrendKey = 'daily' | 'weekly' | 'monthly'

// Fixed Monday–Sunday order for the Busiest Days list.
const WEEKDAY_ORDER = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const

export function AnalyticsPage() {
  const [start, setStart] = useState(defaultStart)
  const [end, setEnd] = useState(defaultEnd)

  const rangeValid = start <= end
  const { data, isPending, isFetching, isError, error, refetch } = useAnalytics(
    start,
    end,
  )

  return (
    <div>
      <PageHeader
        title={strings.nav.analytics}
        actions={
          <RangeControls
            start={start}
            end={end}
            onStart={setStart}
            onEnd={setEnd}
            disabled={isFetching}
          />
        }
      />

      {!rangeValid && (
        <Card>
          <CardContent>
            <p className="text-sm text-destructive">{strings.analytics.rangeInvalid}</p>
          </CardContent>
        </Card>
      )}

      {rangeValid && isPending && <LoadingLayout />}
      {rangeValid && isError && (
        <ErrorState message={error.message} onRetry={() => refetch()} />
      )}
      {rangeValid && data && <AnalyticsLayout data={data} />}
    </div>
  )
}

function RangeControls({
  start,
  end,
  onStart,
  onEnd,
  disabled,
}: {
  start: string
  end: string
  onStart: (v: string) => void
  onEnd: (v: string) => void
  disabled: boolean
}) {
  return (
    <div className="flex flex-wrap items-end gap-3">
      <div>
        <Label htmlFor="analytics-start" className="mb-1">
          {strings.analytics.rangeStart}
        </Label>
        <Input
          id="analytics-start"
          type="date"
          value={start}
          max={end}
          disabled={disabled}
          onChange={(event) => onStart(event.target.value)}
          className="h-10 w-40"
        />
      </div>
      <div>
        <Label htmlFor="analytics-end" className="mb-1">
          {strings.analytics.rangeEnd}
        </Label>
        <Input
          id="analytics-end"
          type="date"
          value={end}
          min={start}
          disabled={disabled}
          onChange={(event) => onEnd(event.target.value)}
          className="h-10 w-40"
        />
      </div>
    </div>
  )
}

function LoadingLayout() {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        {Array.from({ length: 5 }, (_, index) => (
          <Skeleton key={index} className="h-24" />
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Skeleton className="h-72" />
        <Skeleton className="h-72" />
      </div>
      <Skeleton className="h-72" />
    </div>
  )
}

function AnalyticsLayout({ data }: { data: AnalyticsResponse }) {
  const [trend, setTrend] = useState<TrendKey>('daily')

  const trendMap = trendMapFor(data, trend)
  const isEmpty = data.totalReservations === 0

  return (
    <div className="space-y-4">
      <KpiRow data={data} />

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>{strings.analytics.peakHoursTitle}</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <PeakHoursList map={data.peakHours} isEmpty={isEmpty} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{strings.analytics.busiestDaysTitle}</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <BusiestDaysList map={data.busiestDays} isEmpty={isEmpty} />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{strings.analytics.trendsTitle}</CardTitle>
          <TrendToggle value={trend} onChange={setTrend} />
        </CardHeader>
        <CardContent className="p-0">
          <TrendList map={trendMap} isEmpty={isEmpty} />
        </CardContent>
      </Card>
    </div>
  )
}

function KpiRow({ data }: { data: AnalyticsResponse }) {
  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
      <KpiCard
        label={strings.analytics.statTotal}
        value={String(data.totalReservations)}
        icon={<BarChart3 />}
      />
      <KpiCard
        label={strings.analytics.statCompleted}
        value={String(data.completedReservations)}
        icon={<CheckCircle2 />}
      />
      <KpiCard
        label={strings.analytics.statCancelled}
        value={String(data.cancelledReservations)}
        icon={<CalendarX />}
      />
      <KpiCard
        label={strings.analytics.statNoShows}
        value={String(data.noShows)}
        hint={`${data.noShowPercentage.toFixed(1)}% ${strings.analytics.noShowHint}`}
        icon={<UserX />}
      />
      <KpiCard
        label={strings.analytics.statAvgParty}
        value={data.averagePartySize.toFixed(1)}
        icon={<Users />}
      />
    </div>
  )
}

function PeakHoursList({ map, isEmpty }: { map: Record<string, number>; isEmpty: boolean }) {
  const entries = useMemo(() => {
    return Object.entries(map)
      .map(([hour, count]) => ({
        key: hour,
        label: `${hour.padStart(2, '0')}:00`,
        count,
      }))
      .sort((a, b) => b.count - a.count || Number(a.key) - Number(b.key))
  }, [map])

  return <BarList entries={entries} isEmpty={isEmpty || entries.length === 0} icon={<Clock />} />
}

function BusiestDaysList({ map, isEmpty }: { map: Record<string, number>; isEmpty: boolean }) {
  const entries = useMemo(() => {
    return WEEKDAY_ORDER.filter((day) => (map[day] ?? 0) > 0).map((day) => ({
      key: day,
      label: strings.analytics.weekday[day],
      count: map[day] ?? 0,
    }))
  }, [map])

  return (
    <BarList entries={entries} isEmpty={isEmpty || entries.length === 0} icon={<CalendarCheck />} />
  )
}

function TrendList({ map, isEmpty }: { map: Record<string, number>; isEmpty: boolean }) {
  const entries = useMemo(() => {
    return Object.entries(map)
      .map(([key, count]) => ({ key, label: key, count }))
      .sort((a, b) => (a.key < b.key ? -1 : a.key > b.key ? 1 : 0))
  }, [map])

  return (
    <div className="max-h-96 overflow-y-auto">
      <BarList entries={entries} isEmpty={isEmpty || entries.length === 0} icon={<BarChart3 />} />
    </div>
  )
}

function TrendToggle({ value, onChange }: { value: TrendKey; onChange: (v: TrendKey) => void }) {
  const options: Array<{ key: TrendKey; label: string }> = [
    { key: 'daily', label: strings.analytics.trendDaily },
    { key: 'weekly', label: strings.analytics.trendWeekly },
    { key: 'monthly', label: strings.analytics.trendMonthly },
  ]
  return (
    <div className="inline-flex rounded-md border border-input bg-card p-0.5 shadow-xs">
      {options.map((option) => {
        const active = option.key === value
        return (
          <button
            key={option.key}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(option.key)}
            className={cn(
              'rounded-sm px-3 py-1 text-sm font-medium transition-colors',
              active
                ? 'bg-primary text-primary-foreground'
                : 'text-slate-600 hover:bg-muted hover:text-foreground',
            )}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}

interface BarEntry {
  key: string
  label: string
  count: number
}

function BarList({
  entries,
  isEmpty,
  icon,
}: {
  entries: BarEntry[]
  isEmpty: boolean
  icon: ReactNode
}) {
  if (isEmpty) {
    return <EmptyState icon={icon} title={strings.analytics.emptySection} className="py-10" />
  }

  const max = entries.reduce((acc, entry) => Math.max(acc, entry.count), 0)

  return (
    <ul className="divide-y divide-slate-100">
      {entries.map((entry) => {
        const widthPercent = max > 0 ? (entry.count / max) * 100 : 0
        return (
          <li key={entry.key} className="flex items-center gap-3 px-6 py-3">
            <span className="w-24 shrink-0 text-sm font-medium text-foreground">
              {entry.label}
            </span>
            <div
              className="relative h-2 flex-1 overflow-hidden rounded-full bg-muted"
              role="presentation"
            >
              <div
                className="h-full rounded-full bg-brand-600"
                style={{ width: `${widthPercent}%` }}
                aria-hidden="true"
              />
            </div>
            <span
              className="w-12 shrink-0 text-right text-sm font-semibold text-foreground tabular-nums"
              aria-label={`${entry.label}: ${entry.count}`}
            >
              {entry.count}
            </span>
          </li>
        )
      })}
    </ul>
  )
}

function trendMapFor(data: AnalyticsResponse, key: TrendKey): Record<string, number> {
  if (key === 'daily') return data.dailyTrends
  if (key === 'weekly') return data.weeklyTrends
  return data.monthlyTrends
}

function toIsoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const defaultEnd = toIsoDate(new Date())
const defaultStart = (() => {
  const d = new Date()
  d.setDate(d.getDate() - 29)
  return toIsoDate(d)
})()
