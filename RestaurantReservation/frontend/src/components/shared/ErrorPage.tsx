import { Link } from 'react-router-dom'
import { strings } from '@/lib/strings'

function ErrorPage({ title, body }: { title: string; body: string }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 p-4 text-center">
      <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
      <p className="text-sm text-slate-500">{body}</p>
      <Link to="/" className="text-sm font-medium text-brand-600 hover:text-brand-700">
        {strings.notFoundPage.backHome}
      </Link>
    </div>
  )
}

export function ForbiddenPage() {
  return <ErrorPage title={strings.forbiddenPage.title} body={strings.forbiddenPage.body} />
}

export function NotFoundPage() {
  return <ErrorPage title={strings.notFoundPage.title} body={strings.notFoundPage.body} />
}
