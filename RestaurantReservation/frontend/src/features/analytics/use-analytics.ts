import { useQuery } from '@tanstack/react-query'
import { getAnalytics } from '@/api/analytics'

export function useAnalytics(start: string, end: string) {
  return useQuery({
    queryKey: ['analytics', start, end],
    queryFn: () => getAnalytics(start, end),
    enabled: Boolean(start) && Boolean(end) && start <= end,
  })
}
