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
import { RegionSelect } from '@/components/ui/region-select'
import { SOURCE_OPTIONS } from '@/lib/sources'
import type { SubscriptionResponse } from '@/api/types'

const schema = z.object({
  label: z.string().min(1, 'Название обязательно'),
  source: z.string().min(1, 'Выберите источник'),
  customerInn: z.string().optional(),
  maxPriceFrom: z.string()
    .optional()
    .refine((v) => !v || (!isNaN(parseFloat(v)) && parseFloat(v) >= 0), 'Введите корректную сумму'),
  maxPriceTo: z.string()
    .optional()
    .refine((v) => !v || (!isNaN(parseFloat(v)) && parseFloat(v) >= 0), 'Введите корректную сумму'),
}).refine(
  (d) => {
    const from = d.maxPriceFrom ? parseFloat(d.maxPriceFrom) : undefined
    const to = d.maxPriceTo ? parseFloat(d.maxPriceTo) : undefined
    if (from !== undefined && to !== undefined) return from <= to
    return true
  },
  { message: 'Сумма «от» не может быть больше суммы «до»', path: ['maxPriceTo'] }
)

type FormValues = z.infer<typeof schema>

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

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
  const [emailError, setEmailError] = useState<string | null>(null)
  const [keywords, setKeywords] = useState<string[]>(existing?.filters.keywords ?? [])
  const [regions, setRegions] = useState<number[]>(existing?.filters.regions ?? [])

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
        }
      : { source: '', label: '' },
  })

  const sourceValue = watch('source')

  function handleAddEmail(tags: string[]) {
    const last = tags[tags.length - 1]
    if (last && !EMAIL_RE.test(last)) {
      setEmailError(`«${last}» — неверный формат email`)
      setEmails(tags.slice(0, -1))
    } else {
      setEmailError(null)
      setEmails(tags)
    }
  }

  function onValid(values: FormValues) {
    if (emails.length === 0) return
    onSubmit({
      source: values.source,
      label: values.label || undefined,
      emails,
      filters: {
        regions,
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
          Каждое слово — отдельный запрос. Больше слов = больше тендеров.
        </p>
        <TagInput
          value={keywords}
          onChange={setKeywords}
          placeholder="хозтовары, моющие средства…"
        />
      </div>

      {/* Доп. фильтры */}
      <div className="rounded-md border p-4 space-y-4">
        <p className="text-sm font-medium text-muted-foreground">Дополнительные фильтры</p>

        <div className="space-y-1.5">
          <Label>Регионы</Label>
          <RegionSelect value={regions} onChange={setRegions} />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="customerInn">ИНН заказчика</Label>
          <Input id="customerInn" placeholder="1234567890" {...register('customerInn')} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="maxPriceFrom">Сумма от (₽)</Label>
            <Input id="maxPriceFrom" type="number" min="0" placeholder="0" {...register('maxPriceFrom')} />
            {errors.maxPriceFrom && <p className="text-sm text-destructive">{errors.maxPriceFrom.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="maxPriceTo">Сумма до (₽)</Label>
            <Input id="maxPriceTo" type="number" min="0" placeholder="∞" {...register('maxPriceTo')} />
            {errors.maxPriceTo && <p className="text-sm text-destructive">{errors.maxPriceTo.message}</p>}
          </div>
        </div>
      </div>

      {/* Email получателей */}
      <div className="space-y-1.5">
        <Label>Email-адреса получателей</Label>
        <TagInput
          value={emails}
          onChange={handleAddEmail}
          placeholder="user@example.com"
          inputType="email"
        />
        {emailError && <p className="text-sm text-destructive">{emailError}</p>}
        {!emailError && emails.length === 0 && (
          <p className="text-sm text-destructive">Укажите хотя бы один email</p>
        )}
      </div>

      <div className="flex gap-3 pt-2">
        <Button
          type="submit"
          disabled={isLoading || emails.length === 0}
          title={emails.length === 0 ? 'Укажите хотя бы один email' : undefined}
          className="flex-1"
        >
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
