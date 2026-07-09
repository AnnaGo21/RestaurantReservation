import { api } from './client'
import type { DashboardData } from '@/types/dashboard'

export async function getDashboard(): Promise<DashboardData> {
  const { data } = await api.get<DashboardData>('/dashboard')
  return data
}
