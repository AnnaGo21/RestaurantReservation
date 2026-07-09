export type ReservationStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SEATED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export const ALL_RESERVATION_STATUSES: ReservationStatus[] = [
  'PENDING',
  'CONFIRMED',
  'SEATED',
  'COMPLETED',
  'CANCELLED',
  'NO_SHOW',
]

export interface CreateReservationRequest {
  tableId: number
  guestName: string
  guestPhone: string
  guestEmail?: string
  startTime: string
  partySize: number
  notes?: string
}

export interface MoveReservationRequest {
  tableId: number
  startTime: string
}

export interface Reservation {
  id: number
  restaurantId: number
  tableId: number
  guestId: number
  guestName: string
  guestPhone: string
  startTime: string
  endTime: string
  partySize: number
  notes: string | null
  status: ReservationStatus
  reminderSent: boolean
  checkedInAt: string | null
}
