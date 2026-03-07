import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Plus, Pencil, Trash2, BellOff, Search } from 'lucide-react'
import { sourceLabel } from '@/lib/sources'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { SubscriptionDrawer } from './SubscriptionDrawer'
import { getSubscriptions, deleteSubscription, updateSubscriptionStatus } from '@/api/subscriptions'
import type { SubscriptionResponse } from '@/api/types'

function formatDate(iso?: string) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('ru', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso))
}

export function SubscriptionsPage() {
  const qc = useQueryClient()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<SubscriptionResponse | undefined>()
  const [search, setSearch] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['subscriptions'],
    queryFn: getSubscriptions,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteSubscription(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['subscriptions'] })
      toast.success('Подписка удалена')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'ACTIVE' | 'PAUSED' }) =>
      updateSubscriptionStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['subscriptions'] }),
    onError: (e: Error) => toast.error(e.message),
  })

  const filtered = (data ?? []).filter((s) =>
    (s.label ?? s.source).toLowerCase().includes(search.toLowerCase())
  )

  function openCreate() {
    setEditing(undefined)
    setDrawerOpen(true)
  }

  function openEdit(sub: SubscriptionResponse) {
    setEditing(sub)
    setDrawerOpen(true)
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Подписки</h1>
          <p className="text-sm text-muted-foreground">Управление уведомлениями о тендерах</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" />
          Новая подписка
        </Button>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Поиск по названию..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full rounded-md" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 gap-4 text-center">
          <BellOff className="h-10 w-10 text-muted-foreground" />
          <div>
            <p className="font-medium">{search ? 'Ничего не найдено' : 'Нет подписок'}</p>
            <p className="text-sm text-muted-foreground">
              {search ? 'Попробуйте другой запрос' : 'Создайте первую подписку, чтобы получать уведомления'}
            </p>
          </div>
          {!search && (
            <Button onClick={openCreate} variant="outline">
              <Plus className="h-4 w-4" />
              Создать первую подписку
            </Button>
          )}
        </div>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Название</TableHead>
                <TableHead>Источник</TableHead>
                <TableHead>Email-адреса</TableHead>
                <TableHead>Последняя проверка</TableHead>
                <TableHead className="w-20">Активна</TableHead>
                <TableHead className="w-24 text-right">Действия</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((sub) => (
                <TableRow key={sub.id} className={sub.status === 'PAUSED' ? 'opacity-60' : ''}>
                  <TableCell className="font-medium">{sub.label ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant="secondary">{sourceLabel(sub.source)}</Badge>
                  </TableCell>
                  <TableCell className="max-w-[220px]">
                    <span className="block truncate text-sm text-muted-foreground">
                      {sub.emails.join(', ')}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(sub.lastCheckedAt)}
                  </TableCell>
                  <TableCell>
                    <Switch
                      checked={sub.status === 'ACTIVE'}
                      disabled={statusMutation.isPending}
                      onCheckedChange={(checked) =>
                        statusMutation.mutate({ id: sub.id, status: checked ? 'ACTIVE' : 'PAUSED' })
                      }
                    />
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => openEdit(sub)}
                        aria-label="Редактировать"
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>

                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button variant="ghost" size="icon" aria-label="Удалить">
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>Удалить подписку?</AlertDialogTitle>
                            <AlertDialogDescription>
                              Подписка «{sub.label ?? sub.source}» будет удалена без возможности восстановления.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Отмена</AlertDialogCancel>
                            <AlertDialogAction
                              onClick={() => deleteMutation.mutate(sub.id)}
                              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                            >
                              Удалить
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <SubscriptionDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        existing={editing}
      />
    </div>
  )
}
