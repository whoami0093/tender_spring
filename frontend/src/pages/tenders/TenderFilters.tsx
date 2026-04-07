import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import type { TenderFilter, TenderStatus } from '@/api/types'

interface Props {
  value: TenderFilter
  onChange: (filter: TenderFilter) => void
}

const STATUS_OPTIONS: { value: TenderStatus; label: string }[] = [
  { value: 'SENT', label: 'Отправлен' },
  { value: 'IN_PROGRESS', label: 'В обработке' },
  { value: 'WON', label: 'Выигран' },
  { value: 'LOST', label: 'Проигран' },
]

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}

export function TenderFilters({ value, onChange }: Props) {
  const [numberSearch, setNumberSearch] = useState(value.numberSearch ?? '')
  const [customer, setCustomer] = useState(value.customer ?? '')

  const debouncedNumber = useDebounce(numberSearch, 300)
  const debouncedCustomer = useDebounce(customer, 300)

  useEffect(() => {
    onChange({ ...value, numberSearch: debouncedNumber || undefined, page: 0 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedNumber])

  useEffect(() => {
    onChange({ ...value, customer: debouncedCustomer || undefined, page: 0 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedCustomer])

  function handleStatus(val: string) {
    const status = val === 'ALL' ? undefined : ([val] as TenderStatus[])
    onChange({ ...value, status, page: 0 })
  }

  function handleTakenInWork(val: string) {
    const takenInWork = val === 'ALL' ? undefined : val === 'true'
    onChange({ ...value, takenInWork, page: 0 })
  }

  function handleAmountFrom(e: React.ChangeEvent<HTMLInputElement>) {
    const amountFrom = e.target.value ? Number(e.target.value) : undefined
    onChange({ ...value, amountFrom, page: 0 })
  }

  function handleAmountTo(e: React.ChangeEvent<HTMLInputElement>) {
    const amountTo = e.target.value ? Number(e.target.value) : undefined
    onChange({ ...value, amountTo, page: 0 })
  }

  function handleDeadlineFrom(e: React.ChangeEvent<HTMLInputElement>) {
    const deadlineFrom = e.target.value ? new Date(e.target.value).toISOString() : undefined
    onChange({ ...value, deadlineFrom, page: 0 })
  }

  function handleDeadlineTo(e: React.ChangeEvent<HTMLInputElement>) {
    const deadlineTo = e.target.value ? new Date(e.target.value).toISOString() : undefined
    onChange({ ...value, deadlineTo, page: 0 })
  }

  function handleReset() {
    setNumberSearch('')
    setCustomer('')
    onChange({ page: 0, size: value.size, sort: value.sort })
  }

  const hasFilters =
    !!value.numberSearch ||
    !!value.customer ||
    !!value.status?.length ||
    value.takenInWork !== undefined ||
    value.amountFrom !== undefined ||
    value.amountTo !== undefined ||
    !!value.deadlineFrom ||
    !!value.deadlineTo

  return (
    <div className="rounded-lg border bg-card p-4 space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <div className="space-y-1.5">
          <Label>Номер тендера</Label>
          <Input
            placeholder="Поиск по номеру..."
            value={numberSearch}
            onChange={(e) => setNumberSearch(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label>Заказчик</Label>
          <Input
            placeholder="Поиск по заказчику..."
            value={customer}
            onChange={(e) => setCustomer(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label>Статус</Label>
          <Select
            value={value.status?.[0] ?? 'ALL'}
            onValueChange={handleStatus}
          >
            <SelectTrigger>
              <SelectValue placeholder="Все статусы" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Все статусы</SelectItem>
              {STATUS_OPTIONS.map((s) => (
                <SelectItem key={s.value} value={s.value}>
                  {s.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1.5">
          <Label>В работе</Label>
          <Select
            value={value.takenInWork === undefined ? 'ALL' : String(value.takenInWork)}
            onValueChange={handleTakenInWork}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Все</SelectItem>
              <SelectItem value="true">Взято в работу</SelectItem>
              <SelectItem value="false">Не взято</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1.5">
          <Label>Сумма от (₽)</Label>
          <Input
            type="number"
            placeholder="0"
            value={value.amountFrom ?? ''}
            onChange={handleAmountFrom}
          />
        </div>
        <div className="space-y-1.5">
          <Label>Сумма до (₽)</Label>
          <Input
            type="number"
            placeholder="∞"
            value={value.amountTo ?? ''}
            onChange={handleAmountTo}
          />
        </div>
        <div className="space-y-1.5">
          <Label>Дедлайн от</Label>
          <Input
            type="date"
            value={value.deadlineFrom ? value.deadlineFrom.slice(0, 10) : ''}
            onChange={handleDeadlineFrom}
          />
        </div>
        <div className="space-y-1.5">
          <Label>Дедлайн до</Label>
          <Input
            type="date"
            value={value.deadlineTo ? value.deadlineTo.slice(0, 10) : ''}
            onChange={handleDeadlineTo}
          />
        </div>
      </div>
      {hasFilters && (
        <div className="flex justify-end">
          <Button variant="ghost" size="sm" onClick={handleReset}>
            <X className="h-4 w-4 mr-1" />
            Сбросить фильтры
          </Button>
        </div>
      )}
    </div>
  )
}
