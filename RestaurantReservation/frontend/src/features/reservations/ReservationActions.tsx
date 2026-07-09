import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  cancelReservation,
  checkInReservation,
  updateReservationStatus,
} from '@/api/reservations'
import { Button } from '@/components/ui/button'
import { strings } from '@/lib/strings'
import type { Reservation, ReservationStatus } from '@/types/reservation'

type Action = 'checkIn' | 'complete' | 'noShow' | 'cancel'

// Mirrors ReservationStatus.canTransitionTo on the backend.
const actionsByStatus: Record<ReservationStatus, Action[]> = {
  PENDING: ['cancel'],
  CONFIRMED: ['checkIn', 'complete', 'noShow', 'cancel'],
  SEATED: ['complete', 'cancel'],
  COMPLETED: [],
  CANCELLED: [],
  NO_SHOW: [],
}

const requestByAction: Record<Action, (id: number) => Promise<Reservation>> = {
  checkIn: checkInReservation,
  complete: (id) => updateReservationStatus(id, 'COMPLETED'),
  noShow: (id) => updateReservationStatus(id, 'NO_SHOW'),
  cancel: cancelReservation,
}

const variantByAction: Record<Action, 'primary' | 'secondary' | 'danger'> = {
  checkIn: 'primary',
  complete: 'secondary',
  noShow: 'secondary',
  cancel: 'danger',
}

interface ReservationActionsProps {
  reservation: Reservation
  onUpdated: (reservation: Reservation) => void
  onMove: () => void
}

export function ReservationActions({ reservation, onUpdated, onMove }: ReservationActionsProps) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (action: Action) => requestByAction[action](reservation.id),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['reservations'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onUpdated(updated)
    },
  })

  const available = actionsByStatus[reservation.status]
  if (available.length === 0) {
    return null
  }

  return (
    <div className="mt-4 border-t border-slate-200 pt-4">
      {mutation.isError && (
        <p role="alert" className="mb-3 text-sm text-red-600">
          {mutation.error.message}
        </p>
      )}
      <div className="flex flex-wrap gap-2">
        {available.map((action) => (
          <Button
            key={action}
            variant={variantByAction[action]}
            size="sm"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate(action)}
          >
            {strings.reservations.actions[action]}
          </Button>
        ))}
        <Button variant="secondary" size="sm" disabled={mutation.isPending} onClick={onMove}>
          {strings.reservations.actions.move}
        </Button>
      </div>
    </div>
  )
}
