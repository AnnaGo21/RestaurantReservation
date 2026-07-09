import { api } from './client'
import type {
  CreateReservationRequest,
  MoveReservationRequest,
  Reservation,
  ReservationStatus,
} from '@/types/reservation'

export async function getDailyReservations(date: string): Promise<Reservation[]> {
  const { data } = await api.get<Reservation[]>('/reservations/daily', { params: { date } })
  return data
}

export async function createReservation(request: CreateReservationRequest): Promise<Reservation> {
  const { data } = await api.post<Reservation>('/reservations', request)
  return data
}

export async function checkInReservation(id: number): Promise<Reservation> {
  const { data } = await api.patch<Reservation>(`/reservations/${id}/check-in`)
  return data
}

export async function cancelReservation(id: number): Promise<Reservation> {
  const { data } = await api.patch<Reservation>(`/reservations/${id}/cancel`)
  return data
}

export async function updateReservationStatus(
  id: number,
  status: ReservationStatus,
): Promise<Reservation> {
  const { data } = await api.patch<Reservation>(`/reservations/${id}/status`, null, {
    params: { status },
  })
  return data
}

export async function moveReservation(
  id: number,
  request: MoveReservationRequest,
): Promise<Reservation> {
  const { data } = await api.patch<Reservation>(`/reservations/${id}/move`, request)
  return data
}

export async function getAvailableTableIds(startTime: string, endTime: string): Promise<number[]> {
  const { data } = await api.get<number[]>('/reservations/available-tables', {
    params: { startTime, endTime },
  })
  return data
}
