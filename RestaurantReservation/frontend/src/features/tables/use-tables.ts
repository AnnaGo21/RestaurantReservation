import { useQuery } from '@tanstack/react-query'
import { getTables } from '@/api/tables'

// STAFF gets 403 on /api/tables (SecurityConfig) — callers must tolerate
// a failed query and fall back to table ids.
export function useTables() {
  return useQuery({
    queryKey: ['tables'],
    queryFn: getTables,
    retry: false,
  })
}
