import { api } from './client'
import type { CreateTableRequest, RestaurantTable } from '@/types/table'

export async function getTables(): Promise<RestaurantTable[]> {
  const { data } = await api.get<RestaurantTable[]>('/tables')
  return data
}

export async function createTable(body: CreateTableRequest): Promise<RestaurantTable> {
  const { data } = await api.post<RestaurantTable>('/tables', body)
  return data
}

export async function updateTable(id: number, body: CreateTableRequest): Promise<RestaurantTable> {
  const { data } = await api.put<RestaurantTable>(`/tables/${id}`, body)
  return data
}

export async function deleteTable(id: number): Promise<void> {
  await api.delete(`/tables/${id}`)
}
