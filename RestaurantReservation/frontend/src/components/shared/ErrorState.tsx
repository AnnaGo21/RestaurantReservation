import { AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { strings } from '@/lib/strings'

interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center px-6 py-10 text-center">
        <div className="mb-3 flex size-10 items-center justify-center rounded-full bg-red-50 text-red-500">
          <AlertCircle className="size-5" />
        </div>
        <p className="text-sm text-slate-600">{message}</p>
        {onRetry && (
          <Button variant="secondary" size="sm" className="mt-4" onClick={onRetry}>
            {strings.common.retry}
          </Button>
        )}
      </CardContent>
    </Card>
  )
}
