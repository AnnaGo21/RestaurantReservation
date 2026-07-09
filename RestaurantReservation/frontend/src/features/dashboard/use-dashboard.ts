import { useQuery } from '@tanstack/react-query'
import { getDashboard } from '@/api/dashboard'

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
    // Keeps multiple staff devices in sync without WebSockets.
    refetchInterval: 60_000,
  })
}
