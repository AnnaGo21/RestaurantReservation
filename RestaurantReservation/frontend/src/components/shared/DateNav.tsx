import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { formatDateLabel, shiftIsoDate, todayIsoDate } from '@/lib/datetime'
import { strings } from '@/lib/strings'

interface DateNavProps {
  date: string
  onChange: (date: string) => void
}

export function DateNav({ date, onChange }: DateNavProps) {
  return (
    <div className="flex items-center gap-1.5">
      <Button
        variant="secondary"
        size="icon-sm"
        aria-label={strings.common.previousDay}
        onClick={() => onChange(shiftIsoDate(date, -1))}
      >
        <ChevronLeft className="size-4" />
      </Button>
      <span className="min-w-36 px-1 text-center text-sm font-medium text-foreground">
        {formatDateLabel(date)}
      </span>
      <Button
        variant="secondary"
        size="icon-sm"
        aria-label={strings.common.nextDay}
        onClick={() => onChange(shiftIsoDate(date, 1))}
      >
        <ChevronRight className="size-4" />
      </Button>
      <Button variant="secondary" size="sm" onClick={() => onChange(todayIsoDate())}>
        {strings.common.today}
      </Button>
    </div>
  )
}
