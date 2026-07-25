import { Link } from 'react-router-dom'
import { SearchX, ShieldAlert, type LucideIcon } from 'lucide-react'
import { strings } from '@/lib/strings'

function ErrorPage({ title, body, icon: Icon }: { title: string; body: string; icon: LucideIcon }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-2 bg-background p-4 text-center">
      <div className="mb-2 flex size-12 items-center justify-center rounded-full bg-muted text-slate-400">
        <Icon className="size-6" />
      </div>
      <h1 className="text-2xl font-semibold tracking-tight text-foreground">{title}</h1>
      <p className="text-sm text-muted-foreground">{body}</p>
      <Link
        to="/"
        className="mt-4 inline-flex h-9 items-center rounded-md border border-input bg-card px-4 text-sm font-medium text-slate-700 shadow-xs transition-colors hover:bg-slate-50 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40 focus-visible:ring-offset-2"
      >
        {strings.notFoundPage.backHome}
      </Link>
    </div>
  )
}

export function ForbiddenPage() {
  return (
    <ErrorPage
      title={strings.forbiddenPage.title}
      body={strings.forbiddenPage.body}
      icon={ShieldAlert}
    />
  )
}

export function NotFoundPage() {
  return (
    <ErrorPage
      title={strings.notFoundPage.title}
      body={strings.notFoundPage.body}
      icon={SearchX}
    />
  )
}
