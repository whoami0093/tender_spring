import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { TagInput } from '@/components/ui/tag-input'
import { SOURCE_OPTIONS } from '@/lib/sources'
import type { SubscriptionResponse } from '@/api/types'

const schema = z.object({
  label: z.string().min(1, 'Название обязательно'),
  source: z.string().min(1, 'Выберите источник'),
  customerInn: z.string().optional(),
  maxPriceFrom: z.string().optional(),
  maxPriceTo: z.string().optional(),
  regionsRaw: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

function parseRegions(raw: string): number[] {
  return raw
    .split(/[\n,;]+/)
    .map((r) => parseInt(r.trim(), 10))
    .filter((n) => !isNaN(n))
}

interface Props {
  existing?: SubscriptionResponse
  onSubmit: (data: {
    source: string
    label?: string
    emails: string[]
    filters: {
      regions: number[]
      keywords: string[]
      customerInn?: string
      maxPriceFrom?: number
      maxPriceTo?: number
    }
  }) => void
  isLoading: boolean
  onCancel: () => void
}

export function SubscriptionForm({ existing, onSubmit, isLoading, onCancel }: Props) {
  const [emails, setEmails] = useState<string[]>(existing?.emails ?? [])
  const [keywords, setKeywords] = useState<string[]>(existing?.filters.keywords ?? [])

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: existing
      ? {
          label: existing.label ?? '',
          source: existing.source,
          customerInn: existing.filters.customerInn ?? '',
          maxPriceFrom: existing.filters.maxPriceFrom?.toString() ?? '',
          maxPriceTo: existing.filters.maxPriceTo?.toString() ?? '',
          regionsRaw: existing.filters.regions.join(', '),
        }
      : { source: '', label: '', regionsRaw: '' },
  })

  const sourceValue = watch('source')

  function onValid(values: FormValues) {
    if (emails.length === 0) return
    onSubmit({
      source: values.source,
      label: values.label || undefined,
      emails,
      filters: {
        regions: parseRegions(values.regionsRaw ?? ''),
        keywords,
        customerInn: values.customerInn || undefined,
        maxPriceFrom: values.maxPriceFrom ? parseFloat(values.maxPriceFrom) : undefined,
        maxPriceTo: values.maxPriceTo ? parseFloat(values.maxPriceTo) : undefined,
      },
    })
  }

  return (
    <form onSubmit={handleSubmit(onValid)} className="flex flex-col gap-5 mt-4">
      {/* Название */}
      <div className="space-y-1.5">
        <Label htmlFor="label">Название</Label>
        <Input id="label" placeholder="Хоз товары, Омск" {...register('label')} />
        {errors.label && <p className="text-sm text-destructive">{errors.label.message}</p>}
      </div>

      {/* Источник */}
      <div className="space-y-1.5">
        <Label htmlFor="source">Источник</Label>
        <Select
          value={sourceValue}
          onValueChange={(v) => setValue('source', v)}
          disabled={!!existing}
        >
          <SelectTrigger id="source">
            <SelectValue placeholder="Выберите реестр" />
          </SelectTrigger>
          <SelectContent>
            {SOURCE_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        {errors.source && <p className="text-sm text-destructive">{errors.source.message}</p>}
      </div>

      {/* Ключевые слова */}
      <div className="space-y-1.5">
        <Label>Ключевые слова</Label>
        <p className="text-xs text-muted-foreground">
          Каждое слово — отдельный запрос к API. Больше слов = больше тендеров.
        </p>
        <TagInput
          value={keywords}
          onChange={setKeywords}
          placeholder="хозтовары, моющие средства…"
        />
      </div>

      {/* Остальные фильтры */}
      <div className="rounded-md border p-4 space-y-4">
        <p className="text-sm font-medium text-muted-foreground">Дополнительные фильтры</p>

        <div className="space-y-1.5">
          <Label htmlFor="customerInn">ИНН заказчика</Label>
          <Input id="customerInn" placeholder="1234567890" {...register('customerInn')} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="maxPriceFrom">Сумма от (₽)</Label>
            <Input id="maxPriceFrom" type="number" placeholder="0" {...register('maxPriceFrom')} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="maxPriceTo">Сумма до (₽)</Label>
            <Input id="maxPriceTo" type="number" placeholder="∞" {...register('maxPriceTo')} />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="regionsRaw">Регионы (коды через запятую)</Label>
          <Input id="regionsRaw" placeholder="1, 5, 17" {...register('regionsRaw')} />
        </div>
      </div>

      {/* Email получателей */}
      <div className="space-y-1.5">
        <Label>Email-адреса получателей</Label>
        <TagInput
          value={emails}
          onChange={setEmails}
          placeholder="user@example.com"
          inputType="email"
        />
        {emails.length === 0 && (
          <p className="text-sm text-destructive">Укажите хотя бы один email</p>
        )}
      </div>

      <div className="flex gap-3 pt-2">
        <Button type="submit" disabled={isLoading || emails.length === 0} className="flex-1">
          {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
          {existing ? 'Сохранить' : 'Создать'}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel} disabled={isLoading}>
          Отмена
        </Button>
      </div>
    </form>
  )
}
