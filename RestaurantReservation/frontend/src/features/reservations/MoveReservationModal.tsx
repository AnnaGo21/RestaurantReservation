import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getAvailableTableIds, moveReservation } from '@/api/reservations'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useTables } from '@/features/tables/use-tables'
import { addMinutes } from '@/lib/datetime'
import { strings } from '@/lib/strings'
import type { Reservation } from '@/types/reservation'
import { TablePicker } from './CreateReservationModal'

// Preview window only — the backend recomputes the real end time on move.
const PREVIEW_DURATION_MINUTES = 90

const formLabels = strings.reservations.form
const moveLabels = strings.reservations.move

interface MoveReservationModalProps {
  reservation: Reservation
  onClose: () => void
}

export function MoveReservationModal({ reservation, onClose }: MoveReservationModalProps) {
  const queryClient = useQueryClient()
  const tablesQuery = useTables()

  const [date, setDate] = useState(reservation.startTime.slice(0, 10))
  const [time, setTime] = useState(reservation.startTime.slice(11, 16))
  const [tableId, setTableId] = useState<number | null>(reservation.tableId)
  const [fieldError, setFieldError] = useState<string | null>(null)

  const startTime = date && time ? `${date}T${time}:00` : null

  const availabilityQuery = useQuery({
    queryKey: ['available-tables', startTime],
    queryFn: () =>
      getAvailableTableIds(startTime!, addMinutes(startTime!, PREVIEW_DURATION_MINUTES)),
    enabled: startTime !== null,
  })

  // The availability endpoint can't exclude this reservation from conflict
  // checks, so its own table is always offered; the backend validates for real.
  const availableIds = availabilityQuery.data
    ? [...new Set([reservation.tableId, ...availabilityQuery.data])]
    : undefined

  const mutation = useMutation({
    mutationFn: () => moveReservation(reservation.id, { tableId: tableId!, startTime: startTime! }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reservations'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
  })

  function handleTimeChange(nextDate: string, nextTime: string) {
    setDate(nextDate)
    setTime(nextTime)
    setTableId(reservation.tableId)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!startTime) {
      setFieldError(formLabels.required)
      return
    }
    if (tableId === null) {
      setFieldError(formLabels.selectTable)
      return
    }
    setFieldError(null)
    mutation.mutate()
  }

  return (
    <Dialog open onClose={onClose} title={moveLabels.title}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="move-date">{formLabels.date}</Label>
            <Input
              id="move-date"
              type="date"
              value={date}
              onChange={(event) => handleTimeChange(event.target.value, time)}
            />
          </div>
          <div>
            <Label htmlFor="move-time">{formLabels.time}</Label>
            <Input
              id="move-time"
              type="time"
              value={time}
              onChange={(event) => handleTimeChange(date, event.target.value)}
            />
          </div>
        </div>

        <div>
          <Label>{formLabels.table}</Label>
          <TablePicker
            startTime={startTime}
            availableIds={availableIds}
            isLoading={availabilityQuery.isLoading}
            isError={availabilityQuery.isError}
            tables={tablesQuery.data}
            partySize={reservation.partySize}
            selectedId={tableId}
            onSelect={setTableId}
          />
        </div>

        {fieldError && (
          <p role="alert" className="text-sm text-red-600">
            {fieldError}
          </p>
        )}
        {mutation.isError && (
          <p role="alert" className="text-sm text-red-600">
            {mutation.error.message}
          </p>
        )}

        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? moveLabels.submitting : moveLabels.submit}
        </Button>
      </form>
    </Dialog>
  )
}
