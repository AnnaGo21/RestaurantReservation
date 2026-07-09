import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { FullScreenSpinner } from '@/components/ui/spinner'
import { homePathFor } from '@/lib/roles'
import { strings } from '@/lib/strings'
import { useAuth } from './use-auth'

export function LoginPage() {
  const { status, user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (status === 'loading') {
    return <FullScreenSpinner />
  }

  if (status === 'authenticated' && user) {
    return <Navigate to={homePathFor(user.role)} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const loggedInUser = await login({ email, password })
      const from = (location.state as { from?: string } | null)?.from
      navigate(from ?? homePathFor(loggedInUser.role), { replace: true })
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        setError(strings.auth.invalidCredentials)
      } else if (err instanceof Error) {
        setError(err.message)
      } else {
        setError(strings.errors.unexpected)
      }
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-brand-700">{strings.appName}</h1>
          <p className="mt-1 text-sm text-slate-500">{strings.auth.signInTitle}</p>
        </div>
        <Card>
          <CardContent className="py-6">
            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              <div>
                <Label htmlFor="email">{strings.auth.email}</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  required
                  autoFocus
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="password">{strings.auth.password}</Label>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </div>
              {error && (
                <p role="alert" className="text-sm text-red-600">
                  {error}
                </p>
              )}
              <Button type="submit" className="w-full" disabled={submitting}>
                {submitting ? strings.auth.signingIn : strings.auth.signIn}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
