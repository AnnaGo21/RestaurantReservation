import { Hammer } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { strings } from '@/lib/strings'
import { EmptyState } from './EmptyState'
import { PageHeader } from './PageHeader'

export function PagePlaceholder({ title }: { title: string }) {
  return (
    <div>
      <PageHeader title={title} />
      <Card>
        <CardContent className="p-0">
          <EmptyState icon={<Hammer />} title={strings.placeholder.comingSoon} />
        </CardContent>
      </Card>
    </div>
  )
}
