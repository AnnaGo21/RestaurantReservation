import type { Role } from './user'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  role: Role
  restaurantId: number | null
}

export interface AuthResponse {
  token: string
  email: string
  role: string
}
