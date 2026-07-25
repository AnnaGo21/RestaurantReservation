import type { ReactNode } from 'react'
import { Card, CardContent } from '@/components/ui/card'

interface KpiCardProps {
  label: string
  value: string
  hint?: string
  icon?: ReactNode
}

export function KpiCard({ label, value, hint, icon }: KpiCardProps) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-3 px-5 py-4">
        <div className="min-w-0">
          <p className="truncate text-sm text-muted-foreground">{label}</p>
          <p className="mt-1 text-2xl font-semibold tracking-tight text-foreground">{value}</p>
          {hint && <p className="mt-0.5 text-xs text-muted-foreground">{hint}</p>}
        </div>
        {icon && (
          <div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-muted text-slate-500 [&_svg]:size-[1.125rem]">
            {icon}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
