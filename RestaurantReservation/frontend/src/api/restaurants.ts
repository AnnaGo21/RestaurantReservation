import { api } from './client'
import type { Restaurant } from '@/types/restaurant'

export async function getRestaurants(): Promise<Restaurant[]> {
  const response = await api.get<Restaurant[]>('/restaurants')
  return response.data
}
