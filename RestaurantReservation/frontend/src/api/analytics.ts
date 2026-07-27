import { api } from './client'
import type { AnalyticsResponse } from '@/types/analytics'

export async function getAnalytics(
  start: string,
  end: string,
): Promise<AnalyticsResponse> {
  const { data } = await api.get<AnalyticsResponse>('/analytics', {
    params: { start, end },
  })
  return data
}
