import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ArrowUpDown, ArrowUp, ArrowDown, FileSearch } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { getTenders, patchTender } from '@/api/tenders'
import type { TenderFilter, TenderResponse, TenderStatus } from '@/api/types'
import { TenderFilters } from './TenderFilters'

type SortField = 'createdAt' | 'deadline' | 'amount' | 'customer' | 'region' | 'publishedAt'

const STATUS_LABELS: Record<TenderStatus, string> = {
  SENT: 'Отправлен',
  IN_PROGRESS: 'В обработке',
  WON: 'Выигран',
  LOST: 'Проигран',
}

const STATUS_VARIANTS: Record<TenderStatus, 'secondary' | 'outline' | 'default' | 'destructive'> = {
  SENT: 'secondary',
  IN_PROGRESS: 'outline',
  WON: 'default',
  LOST: 'destructive',
}

function formatDate(iso?: string | null) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('ru', { dateStyle: 'short' }).format(new Date(iso))
}

function formatAmount(amount?: number | null, currency?: string) {
  if (amount === null || amount === undefined) return '—'
  return new Intl.NumberFormat('ru', { style: 'currency', currency: currency ?? 'RUB', maximumFractionDigits: 0 }).format(amount)
}

function SortIcon({ field, sort }: { field: SortField; sort: string }) {
  const [sortField, dir] = sort.split(',')
  if (sortField !== field) return <ArrowUpDown className="ml-1 h-3.5 w-3.5 text-muted-foreground" />
  return dir === 'asc'
    ? <ArrowUp className="ml-1 h-3.5 w-3.5" />
    : <ArrowDown className="ml-1 h-3.5 w-3.5" />
}

export function TendersPage() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState<TenderFilter>({ page: 0, size: 20, sort: 'createdAt,desc' })

  const { data, isLoading } = useQuery({
    queryKey: ['tenders', filter],
    queryFn: () => getTenders(filter),
  })

  const patchMutation = useMutation({
    mutationFn: ({ id, takenInWork }: { id: number; takenInWork: boolean }) =>
      patchTender(id, takenInWork),
    onMutate: async ({ id, takenInWork }) => {
      await qc.cancelQueries({ queryKey: ['tenders', filter] })
      const previous = qc.getQueryData(['tenders', filter])
      qc.setQueryData(['tenders', filter], (old: typeof data) => {
        if (!old) return old
        return { ...old, content: old.content.map((t: TenderResponse) => t.id === id ? { ...t, takenInWork } : t) }
      })
      return { previous }
    },
    onError: (_err, _vars, ctx) => {
      qc.setQueryData(['tenders', filter], ctx?.previous)
      toast.error('Не удалось обновить тендер')
    },
    onSettled: () => qc.invalidateQueries({ queryKey: ['tenders', filter] }),
  })

  function handleSort(field: SortField) {
    const [currentField, currentDir] = (filter.sort ?? 'createdAt,desc').split(',')
    const newDir = currentField === field && currentDir === 'desc' ? 'asc' : 'desc'
    setFilter((f) => ({ ...f, sort: `${field},${newDir}`, page: 0 }))
  }

  function handleFilterChange(newFilter: TenderFilter) {
    setFilter((f) => ({ ...f, ...newFilter, sort: f.sort }))
  }

  function SortableHead({ field, children }: { field: SortField; children: React.ReactNode }) {
    return (
      <TableHead
        className="cursor-pointer select-none hover:text-foreground"
        onClick={() => handleSort(field)}
      >
        <span className="inline-flex items-center">
          {children}
          <SortIcon field={field} sort={filter.sort ?? 'createdAt,desc'} />
        </span>
      </TableHead>
    )
  }

  const totalPages = data?.totalPages ?? 1
  const currentPage = filter.page ?? 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Тендеры</h1>
        <p className="text-sm text-muted-foreground">
          Список отправленных тендеров
          {data && ` — ${data.totalElements} шт.`}
        </p>
      </div>

      {/* Filters */}
      <TenderFilters value={filter} onChange={handleFilterChange} />

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full rounded-md" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 gap-4 text-center">
          <FileSearch className="h-10 w-10 text-muted-foreground" />
          <div>
            <p className="font-medium">Тендеры не найдены</p>
            <p className="text-sm text-muted-foreground">Попробуйте изменить фильтры</p>
          </div>
        </div>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[140px]">Номер</TableHead>
                <TableHead>Название</TableHead>
                <SortableHead field="region">Регион</SortableHead>
                <SortableHead field="customer">Заказчик</SortableHead>
                <SortableHead field="amount">Сумма</SortableHead>
                <TableHead>Статус</TableHead>
                <SortableHead field="deadline">Дедлайн</SortableHead>
                <TableHead className="w-28">В работе</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((tender) => (
                <TableRow key={tender.id}>
                  <TableCell className="font-mono text-xs text-muted-foreground">
                    {tender.eisUrl ? (
                      <a
                        href={tender.eisUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="hover:underline text-primary"
                      >
                        {tender.purchaseNumber}
                      </a>
                    ) : (
                      tender.purchaseNumber
                    )}
                  </TableCell>
                  <TableCell className="max-w-[260px]">
                    <span className="block truncate" title={tender.title}>
                      {tender.title}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {tender.region ?? '—'}
                  </TableCell>
                  <TableCell className="max-w-[200px] text-sm text-muted-foreground">
                    <span className="block truncate" title={tender.customer ?? ''}>
                      {tender.customer ?? '—'}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm tabular-nums">
                    {formatAmount(tender.amount, tender.currency)}
                  </TableCell>
                  <TableCell>
                    <Badge variant={STATUS_VARIANTS[tender.status]}>
                      {STATUS_LABELS[tender.status]}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(tender.deadline)}
                  </TableCell>
                  <TableCell>
                    <Switch
                      checked={tender.takenInWork}
                      disabled={patchMutation.isPending}
                      onCheckedChange={(checked) =>
                        patchMutation.mutate({ id: tender.id, takenInWork: checked })
                      }
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Страница {currentPage + 1} из {totalPages} (всего {data.totalElements})
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === 0}
              onClick={() => setFilter((f) => ({ ...f, page: currentPage - 1 }))}
            >
              Назад
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage >= totalPages - 1}
              onClick={() => setFilter((f) => ({ ...f, page: currentPage + 1 }))}
            >
              Вперёд
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
