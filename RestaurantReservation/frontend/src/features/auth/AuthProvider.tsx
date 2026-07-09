import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { setOnUnauthorized } from '@/api/client'
import { login as loginRequest } from '@/api/auth'
import { getMe } from '@/api/users'
import { tokenStorage } from '@/lib/storage'
import type { LoginRequest } from '@/types/auth'
import type { AppUser } from '@/types/user'
import { AuthContext, type AuthContextValue } from './auth-context'

type AuthState =
  | { status: 'loading'; user: null }
  | { status: 'authenticated'; user: AppUser }
  | { status: 'unauthenticated'; user: null }

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [state, setState] = useState<AuthState>(() =>
    tokenStorage.get()
      ? { status: 'loading', user: null }
      : { status: 'unauthenticated', user: null },
  )

  useEffect(() => {
    setOnUnauthorized(() => setState({ status: 'unauthenticated', user: null }))
    return () => setOnUnauthorized(null)
  }, [])

  // Session restore: the stored token is only trusted after /users/me accepts it.
  useEffect(() => {
    if (!tokenStorage.get()) {
      return
    }
    let cancelled = false
    getMe()
      .then((user) => {
        if (!cancelled) {
          setState({ status: 'authenticated', user })
        }
      })
      .catch(() => {
        if (!cancelled) {
          tokenStorage.clear()
          setState({ status: 'unauthenticated', user: null })
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (request: LoginRequest): Promise<AppUser> => {
    const response = await loginRequest(request)
    tokenStorage.set(response.token)
    try {
      const user = await getMe()
      setState({ status: 'authenticated', user })
      return user
    } catch (error) {
      tokenStorage.clear()
      throw error
    }
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    // Drop all cached server data so nothing leaks to the next login on a shared device.
    queryClient.clear()
    setState({ status: 'unauthenticated', user: null })
  }, [queryClient])

  const value = useMemo<AuthContextValue>(
    () => ({ status: state.status, user: state.user, login, logout }),
    [state, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
