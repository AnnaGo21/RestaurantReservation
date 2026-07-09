import { api } from './client'
import type { RestaurantTable } from '@/types/table'

export async function getTables(): Promise<RestaurantTable[]> {
  const { data } = await api.get<RestaurantTable[]>('/tables')
  return data
}
