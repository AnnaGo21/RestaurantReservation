import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

export function ListSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <Card>
      <CardContent className="p-0">
        <div className="divide-y divide-slate-100">
          {Array.from({ length: rows }, (_, index) => (
            <div key={index} className="flex items-center gap-4 px-6 py-3.5">
              <Skeleton className="h-4 w-12" />
              <Skeleton className="h-4 w-full max-w-48" />
              <Skeleton className="ml-auto h-4 w-20" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
