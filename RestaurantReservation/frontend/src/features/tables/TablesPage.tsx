import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Armchair, Pencil, Plus, Trash2 } from 'lucide-react'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import { ListSkeleton } from '@/components/shared/ListSkeleton'
import { PageHeader } from '@/components/shared/PageHeader'
import { SectionHeader } from '@/components/shared/SectionHeader'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useToast } from '@/components/ui/toast-context'
import { deleteTable } from '@/api/tables'
import { strings } from '@/lib/strings'
import type { RestaurantTable, TableStatus } from '@/types/table'
import { TableFormModal } from './TableFormModal'
import { useTables } from './use-tables'

const s = strings.tables

const statusClasses: Record<TableStatus, string> = {
  AVAILABLE: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  RESERVED: 'border-blue-200 bg-blue-50 text-blue-700',
  OUT_OF_SERVICE: 'border-slate-200 bg-slate-100 text-slate-600',
}

export function TablesPage() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const { data: tables, isPending, isError, error, refetch } = useTables()

  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState<RestaurantTable | null>(null)
  const [deleting, setDeleting] = useState<RestaurantTable | null>(null)

  const deleteMutation = useMutation({
    mutationFn: deleteTable,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tables'] })
      setDeleting(null)
      toast({ title: s.deleted })
    },
  })

  const tablesWithPos = (tables ?? []).filter(
    (t) => t.positionX !== null && t.positionY !== null,
  )

  return (
    <div>
      <PageHeader
        title={strings.nav.tables}
        actions={
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" />
            {s.newButton}
          </Button>
        }
      />

      {isPending && <ListSkeleton rows={5} />}

      {isError && <ErrorState message={error.message} onRetry={() => refetch()} />}

      {tables && (
        <>
          <Card>
            <CardContent className="p-0">
              {tables.length === 0 ? (
                <EmptyState icon={<Armchair />} title={s.empty} />
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                        <th className="px-4 py-3">{s.colLabel}</th>
                        <th className="px-4 py-3">{s.colCapacity}</th>
                        <th className="px-4 py-3">{s.colStatus}</th>
                        <th className="px-4 py-3">{s.colPosition}</th>
                        <th className="px-4 py-3" />
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {tables.map((table) => (
                        <tr key={table.id} className="transition-colors hover:bg-slate-50">
                          <td className="px-4 py-3 font-medium text-foreground">{table.label}</td>
                          <td className="px-4 py-3 text-slate-600">
                            {table.capacity} {strings.dashboard.seatsSuffix}
                          </td>
                          <td className="px-4 py-3">
                            <span
                              className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${statusClasses[table.status]}`}
                            >
                              {s.status[table.status]}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-slate-500 tabular-nums">
                            {table.positionX !== null && table.positionY !== null
                              ? `${table.positionX}, ${table.positionY}`
                              : '—'}
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex justify-end gap-1.5">
                              <Button
                                variant="ghost"
                                size="icon-sm"
                                aria-label={strings.common.edit}
                                onClick={() => setEditing(table)}
                              >
                                <Pencil className="size-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon-sm"
                                aria-label={s.deleteConfirm}
                                onClick={() => setDeleting(table)}
                                className="text-red-500 hover:bg-red-50 hover:text-red-600"
                              >
                                <Trash2 className="size-4" />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>

          {tablesWithPos.length > 0 && (
            <div className="mt-8">
              <SectionHeader title={s.floorTitle} />
              <Card>
                <CardContent className="p-4">
                  <FloorView tables={tablesWithPos} onEdit={setEditing} />
                </CardContent>
              </Card>
            </div>
          )}
        </>
      )}

      {createOpen && <TableFormModal onClose={() => setCreateOpen(false)} />}
      {editing && <TableFormModal table={editing} onClose={() => setEditing(null)} />}

      <ConfirmDialog
        open={Boolean(deleting)}
        title={s.deleteTitle}
        body={
          deleting ? (
            <>
              {s.deleteBody}{' '}
              <span className="font-semibold">&ldquo;{deleting.label}&rdquo;</span>?
            </>
          ) : null
        }
        confirmLabel={s.deleteConfirm}
        pendingLabel={s.deleting}
        destructive
        isPending={deleteMutation.isPending}
        error={deleteMutation.error?.message ?? null}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </div>
  )
}

const CANVAS_W = 640
const CANVAS_H = 400

// Tables grow with capacity. Larger parties get a wider footprint on the map
// so staff can eyeball 2-tops vs. 6-tops at a glance.
function boxSize(capacity: number): { w: number; h: number } {
  const clamped = Math.max(1, capacity)
  return {
    w: Math.round(40 + clamped * 6),
    h: Math.round(32 + clamped * 3),
  }
}

function FloorView({
  tables,
  onEdit,
}: {
  tables: RestaurantTable[]
  onEdit: (table: RestaurantTable) => void
}) {
  const maxX = Math.max(...tables.map((t) => t.positionX!), 1)
  const maxY = Math.max(...tables.map((t) => t.positionY!), 1)
  const maxBox = tables.reduce(
    (acc, t) => {
      const { w, h } = boxSize(t.capacity)
      return { w: Math.max(acc.w, w), h: Math.max(acc.h, h) }
    },
    { w: 0, h: 0 },
  )
  const scaleX = (CANVAS_W - maxBox.w) / maxX
  const scaleY = (CANVAS_H - maxBox.h) / maxY

  return (
    <div
      className="relative overflow-auto rounded-md border border-border bg-muted/60"
      style={{ width: CANVAS_W, height: CANVAS_H }}
    >
      {tables.map((table) => {
        const { w, h } = boxSize(table.capacity)
        return (
          <button
            key={table.id}
            type="button"
            onClick={() => onEdit(table)}
            title={`Edit ${table.label}`}
            style={{
              left: table.positionX! * scaleX,
              top: table.positionY! * scaleY,
              width: w,
              height: h,
            }}
            className={`absolute flex flex-col items-center justify-center rounded-md border text-xs font-medium transition-opacity hover:opacity-80 ${statusClasses[table.status]}`}
          >
            <span className="truncate px-1">{table.label}</span>
            <span className="opacity-70">{table.capacity}p</span>
          </button>
        )
      })}
    </div>
  )
}
