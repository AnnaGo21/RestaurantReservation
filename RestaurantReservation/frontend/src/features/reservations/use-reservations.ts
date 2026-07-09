import { useQuery } from '@tanstack/react-query'
import { getDailyReservations } from '@/api/reservations'

export function useDailyReservations(date: string) {
  return useQuery({
    queryKey: ['reservations', 'daily', date],
    queryFn: () => getDailyReservations(date),
    refetchInterval: 60_000,
  })
}
