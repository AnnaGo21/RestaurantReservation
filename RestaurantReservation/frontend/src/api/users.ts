import { api } from './client'
import type { AppUser } from '@/types/user'

export async function getMe(): Promise<AppUser> {
  const { data } = await api.get<AppUser>('/users/me')
  return data
}
