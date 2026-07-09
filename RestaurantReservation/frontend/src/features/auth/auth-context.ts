import { createContext } from 'react'
import type { LoginRequest } from '@/types/auth'
import type { AppUser } from '@/types/user'

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface AuthContextValue {
  status: AuthStatus
  user: AppUser | null
  login: (request: LoginRequest) => Promise<AppUser>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
