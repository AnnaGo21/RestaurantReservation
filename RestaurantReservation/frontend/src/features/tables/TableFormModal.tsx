import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createTable, updateTable } from '@/api/tables'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { strings } from '@/lib/strings'
import type { CreateTableRequest, RestaurantTable, TableStatus } from '@/types/table'

const labels = strings.tables.form
const ALL_STATUSES: TableStatus[] = ['AVAILABLE', 'RESERVED', 'OUT_OF_SERVICE']

interface Props {
  table?: RestaurantTable
  onClose: () => void
}

export function TableFormModal({ table, onClose }: Props) {
  const queryClient = useQueryClient()
  const isEdit = table !== undefined

  const [label, setLabel] = useState(table?.label ?? '')
  const [capacity, setCapacity] = useState(String(table?.capacity ?? '2'))
  const [status, setStatus] = useState<TableStatus>(table?.status ?? 'AVAILABLE')
  const [posX, setPosX] = useState(table?.positionX != null ? String(table.positionX) : '')
  const [posY, setPosY] = useState(table?.positionY != null ? String(table.positionY) : '')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const mutation = useMutation({
    mutationFn: (payload: CreateTableRequest) =>
      isEdit ? updateTable(table!.id, payload) : createTable(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tables'] })
      onClose()
    },
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const errors: Record<string, string> = {}
    const cap = Number(capacity)
    if (!label.trim()) errors.label = labels.required
    if (!Number.isInteger(cap) || cap < 1) errors.capacity = labels.capacityInvalid
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    mutation.mutate({
      label: label.trim(),
      capacity: cap,
      status,
      positionX: posX !== '' ? Number(posX) : null,
      positionY: posY !== '' ? Number(posY) : null,
    })
  }

  return (
    <Dialog open onClose={onClose} title={isEdit ? labels.editTitle : labels.createTitle}>
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div>
          <Label htmlFor="tbl-label">{labels.label}</Label>
          <Input
            id="tbl-label"
            autoFocus
            value={label}
            onChange={(e) => setLabel(e.target.value)}
          />
          <FieldError message={fieldErrors.label} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="tbl-capacity">{labels.capacity}</Label>
            <Input
              id="tbl-capacity"
              type="number"
              min={1}
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
            />
            <FieldError message={fieldErrors.capacity} />
          </div>
          <div>
            <Label htmlFor="tbl-status">{labels.status}</Label>
            <Select
              id="tbl-status"
              value={status}
              onChange={(e) => setStatus(e.target.value as TableStatus)}
              className="w-full"
            >
              {ALL_STATUSES.map((st) => (
                <option key={st} value={st}>
                  {strings.tables.status[st]}
                </option>
              ))}
            </Select>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="tbl-posX">{labels.positionX}</Label>
            <Input
              id="tbl-posX"
              type="number"
              value={posX}
              placeholder="—"
              onChange={(e) => setPosX(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="tbl-posY">{labels.positionY}</Label>
            <Input
              id="tbl-posY"
              type="number"
              value={posY}
              placeholder="—"
              onChange={(e) => setPosY(e.target.value)}
            />
          </div>
        </div>

        {mutation.isError && (
          <p role="alert" className="text-sm text-red-600">
            {mutation.error.message}
          </p>
        )}

        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? labels.submitting : labels.submit}
        </Button>
      </form>
    </Dialog>
  )
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null
  return <p className="mt-1 text-xs text-red-600">{message}</p>
}
