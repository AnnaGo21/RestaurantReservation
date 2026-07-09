import axios, { AxiosError } from 'axios'
import { tokenStorage } from '@/lib/storage'
import { strings } from '@/lib/strings'

// Matches the backend's GlobalExceptionHandler ErrorResponse body.
interface BackendErrorBody {
  status?: number
  message?: string
  timestamp?: string
}

export class ApiError extends Error {
  readonly status: number | undefined

  constructor(message: string, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
})

let onUnauthorized: (() => void) | null = null

// Registered by AuthProvider so an expired/invalid token anywhere in the app
// ends the session through React state instead of a hard page reload.
export function setOnUnauthorized(handler: (() => void) | null): void {
  onUnauthorized = handler
}

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<BackendErrorBody>) => {
    const status = error.response?.status
    const isAuthEndpoint = error.config?.url?.includes('/auth/') ?? false

    if (status === 401 && !isAuthEndpoint) {
      tokenStorage.clear()
      onUnauthorized?.()
    }

    return Promise.reject(new ApiError(resolveMessage(error), status))
  },
)

function resolveMessage(error: AxiosError<BackendErrorBody>): string {
  if (!error.response) {
    return strings.errors.network
  }
  const backendMessage = error.response.data?.message
  if (backendMessage) {
    return backendMessage
  }
  switch (error.response.status) {
    case 403:
      return strings.errors.forbidden
    case 404:
      return strings.errors.notFound
    default:
      return strings.errors.unexpected
  }
}
