import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createReservation, getAvailableTableIds } from '@/api/reservations'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import { Textarea } from '@/components/ui/textarea'
import { useTables } from '@/features/tables/use-tables'
import { addMinutes } from '@/lib/datetime'
import { strings } from '@/lib/strings'
import { cn } from '@/lib/utils'

// Availability preview only — the backend recomputes the real end time from
// the restaurant's configured duration on create.
const DEFAULT_DURATION_MINUTES = 90

const labels = strings.reservations.form

interface CreateReservationModalProps {
  defaultDate: string
  onClose: () => void
}

export function CreateReservationModal({ defaultDate, onClose }: CreateReservationModalProps) {
  const queryClient = useQueryClient()
  const tablesQuery = useTables()

  const [guestName, setGuestName] = useState('')
  const [guestPhone, setGuestPhone] = useState('')
  const [partySize, setPartySize] = useState('2')
  const [date, setDate] = useState(defaultDate)
  const [time, setTime] = useState('')
  const [notes, setNotes] = useState('')
  const [tableId, setTableId] = useState<number | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const startTime = date && time ? `${date}T${time}:00` : null

  const availabilityQuery = useQuery({
    queryKey: ['available-tables', startTime],
    queryFn: () =>
      getAvailableTableIds(startTime!, addMinutes(startTime!, DEFAULT_DURATION_MINUTES)),
    enabled: startTime !== null,
  })

  const mutation = useMutation({
    mutationFn: createReservation,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reservations'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
  })

  function handleTimeChange(nextDate: string, nextTime: string) {
    setDate(nextDate)
    setTime(nextTime)
    setTableId(null)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const errors: Record<string, string> = {}
    const party = Number(partySize)
    if (!guestName.trim()) errors.guestName = labels.required
    if (!guestPhone.trim()) errors.guestPhone = labels.required
    if (!Number.isInteger(party) || party < 1) errors.partySize = labels.partySizeInvalid
    if (!startTime) errors.time = labels.required
    if (tableId === null) errors.tableId = labels.selectTable
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0 || !startTime || tableId === null) {
      return
    }
    mutation.mutate({
      tableId,
      guestName: guestName.trim(),
      guestPhone: guestPhone.trim(),
      startTime,
      partySize: party,
      notes: notes.trim() || undefined,
    })
  }

  return (
    <Dialog open onClose={onClose} title={labels.title}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div>
          <Label htmlFor="guestName">{labels.guestName}</Label>
          <Input
            id="guestName"
            autoFocus
            value={guestName}
            onChange={(event) => setGuestName(event.target.value)}
          />
          <FieldError message={fieldErrors.guestName} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="guestPhone">{labels.guestPhone}</Label>
            <Input
              id="guestPhone"
              type="tel"
              value={guestPhone}
              onChange={(event) => setGuestPhone(event.target.value)}
            />
            <FieldError message={fieldErrors.guestPhone} />
          </div>
          <div>
            <Label htmlFor="partySize">{labels.partySize}</Label>
            <Input
              id="partySize"
              type="number"
              min={1}
              value={partySize}
              onChange={(event) => setPartySize(event.target.value)}
            />
            <FieldError message={fieldErrors.partySize} />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="date">{labels.date}</Label>
            <Input
              id="date"
              type="date"
              value={date}
              onChange={(event) => handleTimeChange(event.target.value, time)}
            />
          </div>
          <div>
            <Label htmlFor="time">{labels.time}</Label>
            <Input
              id="time"
              type="time"
              value={time}
              onChange={(event) => handleTimeChange(date, event.target.value)}
            />
          </div>
        </div>
        <FieldError message={fieldErrors.time} />

        <div>
          <Label>{labels.table}</Label>
          <TablePicker
            startTime={startTime}
            availableIds={availabilityQuery.data}
            isLoading={availabilityQuery.isLoading}
            isError={availabilityQuery.isError}
            tables={tablesQuery.data}
            partySize={Number(partySize)}
            selectedId={tableId}
            onSelect={setTableId}
          />
          <FieldError message={fieldErrors.tableId} />
        </div>

        <div>
          <Label htmlFor="notes">{labels.notes}</Label>
          <Textarea
            id="notes"
            rows={2}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
          />
        </div>

        {mutation.isError && (
          <p role="alert" className="text-sm text-red-600">
            {mutation.error.message}
          </p>
        )}

        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? labels.submitting : labels.submit}
        </Button>
      </form>
    </Dialog>
  )
}

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null
  }
  return <p className="mt-1 text-xs text-red-600">{message}</p>
}

export function TablePicker({
  startTime,
  availableIds,
  isLoading,
  isError,
  tables,
  partySize,
  selectedId,
  onSelect,
}: {
  startTime: string | null
  availableIds: number[] | undefined
  isLoading: boolean
  isError: boolean
  tables: { id: number; label: string; capacity: number }[] | undefined
  partySize: number
  selectedId: number | null
  onSelect: (id: number) => void
}) {
  if (!startTime) {
    return <p className="text-sm text-slate-500">{labels.pickTimeFirst}</p>
  }
  if (isLoading) {
    return <Spinner className="size-5" />
  }
  if (isError) {
    return <p className="text-sm text-red-600">{labels.loadTablesFailed}</p>
  }
  if (!availableIds || availableIds.length === 0) {
    return <p className="text-sm text-slate-500">{labels.noTablesAvailable}</p>
  }

  const tableById = new Map((tables ?? []).map((table) => [table.id, table]))

  return (
    <div className="flex flex-wrap gap-2">
      {availableIds.map((id) => {
        const table = tableById.get(id)
        const tooSmall = table !== undefined && partySize >= 1 && table.capacity < partySize
        return (
          <button
            key={id}
            type="button"
            disabled={tooSmall}
            onClick={() => onSelect(id)}
            className={cn(
              'inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm font-medium transition-colors',
              selectedId === id
                ? 'border-brand-600 bg-brand-600 text-white'
                : 'border-slate-300 bg-white text-slate-700 hover:border-brand-400',
              tooSmall && 'pointer-events-none opacity-40',
            )}
          >
            {table?.label ?? `#${id}`}
            {table && (
              <span className={cn('text-xs', selectedId === id ? 'text-brand-100' : 'text-slate-400')}>
                {table.capacity} {strings.dashboard.seatsSuffix}
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
