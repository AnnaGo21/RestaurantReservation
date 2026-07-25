import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { strings } from '@/lib/strings'

interface ConfirmDialogProps {
  open: boolean
  title: string
  body: ReactNode
  confirmLabel: string
  pendingLabel?: string
  destructive?: boolean
  isPending?: boolean
  error?: string | null
  onConfirm: () => void
  onClose: () => void
}

export function ConfirmDialog({
  open,
  title,
  body,
  confirmLabel,
  pendingLabel,
  destructive = false,
  isPending = false,
  error,
  onConfirm,
  onClose,
}: ConfirmDialogProps) {
  const close = () => {
    if (!isPending) {
      onClose()
    }
  }

  return (
    <Dialog
      open={open}
      onClose={close}
      title={title}
      footer={
        <>
          <Button variant="secondary" className="flex-1" disabled={isPending} onClick={close}>
            {strings.common.cancel}
          </Button>
          <Button
            variant={destructive ? 'danger' : 'primary'}
            className="flex-1"
            disabled={isPending}
            onClick={onConfirm}
          >
            {isPending ? (pendingLabel ?? confirmLabel) : confirmLabel}
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <div className="text-sm text-slate-700">{body}</div>
        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        )}
      </div>
    </Dialog>
  )
}
