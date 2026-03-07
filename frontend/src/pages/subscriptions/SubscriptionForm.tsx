import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { SubscriptionResponse } from '@/api/types'

const schema = z.object({
  label: z.string().min(1, 'Название обязательно'),
  source: z.string().min(1, 'Выберите источник'),
  emailsRaw: z.string().min(1, 'Укажите хотя бы один email'),
  objectInfo: z.string().optional(),
  customerInn: z.string().optional(),
  maxPriceFrom: z.string().optional(),
  maxPriceTo: z.string().optional(),
  regionsRaw: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

function parseEmails(raw: string): string[] {
  return raw
    .split(/[\n,;]+/)
    .map((e) => e.trim())
    .filter(Boolean)
}

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
      objectInfo?: string
      customerInn?: string
      maxPriceFrom?: number
      maxPriceTo?: number
    }
  }) => void
  isLoading: boolean
  onCancel: () => void
}

export function SubscriptionForm({ existing, onSubmit, isLoading, onCancel }: Props) {
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
          emailsRaw: existing.emails.join('\n'),
          objectInfo: existing.filters.objectInfo ?? '',
          customerInn: existing.filters.customerInn ?? '',
          maxPriceFrom: existing.filters.maxPriceFrom?.toString() ?? '',
          maxPriceTo: existing.filters.maxPriceTo?.toString() ?? '',
          regionsRaw: existing.filters.regions.join(', '),
        }
      : { source: 'gosplan', label: '', emailsRaw: '', regionsRaw: '' },
  })

  const sourceValue = watch('source')

  function onValid(values: FormValues) {
    const emails = parseEmails(values.emailsRaw)
    const regions = parseRegions(values.regionsRaw ?? '')
    onSubmit({
      source: values.source,
      label: values.label || undefined,
      emails,
      filters: {
        regions,
        objectInfo: values.objectInfo || undefined,
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
        <Input id="label" placeholder="Строительство, регион X" {...register('label')} />
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
            <SelectItem value="gosplan">Госплан</SelectItem>
          </SelectContent>
        </Select>
        {errors.source && <p className="text-sm text-destructive">{errors.source.message}</p>}
      </div>

      {/* Фильтры */}
      <div className="rounded-md border p-4 space-y-4">
        <p className="text-sm font-medium text-muted-foreground">Фильтры тендеров</p>

        <div className="space-y-1.5">
          <Label htmlFor="objectInfo">Ключевые слова (предмет закупки)</Label>
          <Input id="objectInfo" placeholder="Ремонт дороги" {...register('objectInfo')} />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="customerInn">ИНН заказчика</Label>
          <Input id="customerInn" placeholder="1234567890" {...register('customerInn')} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="maxPriceFrom">Сумма от (₸)</Label>
            <Input id="maxPriceFrom" type="number" placeholder="0" {...register('maxPriceFrom')} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="maxPriceTo">Сумма до (₸)</Label>
            <Input id="maxPriceTo" type="number" placeholder="∞" {...register('maxPriceTo')} />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="regionsRaw">Регионы (коды через запятую)</Label>
          <Input id="regionsRaw" placeholder="1, 5, 17" {...register('regionsRaw')} />
        </div>
      </div>

      {/* Email-список */}
      <div className="space-y-1.5">
        <Label htmlFor="emailsRaw">Email-адреса получателей</Label>
        <Textarea
          id="emailsRaw"
          placeholder="one@example.com&#10;two@example.com"
          rows={4}
          {...register('emailsRaw')}
        />
        {errors.emailsRaw && <p className="text-sm text-destructive">{errors.emailsRaw.message}</p>}
      </div>

      <div className="flex gap-3 pt-2">
        <Button type="submit" disabled={isLoading} className="flex-1">
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
