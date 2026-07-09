import { Card, CardContent } from '@/components/ui/card'
import { strings } from '@/lib/strings'

export function PagePlaceholder({ title }: { title: string }) {
  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-slate-900">{title}</h1>
      <Card>
        <CardContent className="py-12 text-center text-sm text-slate-500">
          {strings.placeholder.comingSoon}
        </CardContent>
      </Card>
    </div>
  )
}
