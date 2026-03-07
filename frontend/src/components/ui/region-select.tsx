import * as React from 'react'
import * as Popover from '@radix-ui/react-popover'
import { Check, ChevronsUpDown, X } from 'lucide-react'
import { cn } from '@/lib/utils'
import { REGIONS, REGION_BY_CODE } from '@/data/regions'

interface RegionSelectProps {
  value: number[]
  onChange: (regions: number[]) => void
}

export function RegionSelect({ value, onChange }: RegionSelectProps) {
  const [open, setOpen] = React.useState(false)
  const [search, setSearch] = React.useState('')

  const filtered = REGIONS.filter((r) =>
    r.name.toLowerCase().includes(search.toLowerCase()) ||
    r.code.toString().includes(search)
  )

  function toggle(code: number) {
    onChange(value.includes(code) ? value.filter((c) => c !== code) : [...value, code])
  }

  function removeRegion(code: number) {
    onChange(value.filter((c) => c !== code))
  }

  return (
    <div className="space-y-2">
      {value.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {value.map((code) => {
            const region = REGION_BY_CODE.get(code)
            return (
              <span
                key={code}
                className="inline-flex items-center gap-1 rounded-md bg-secondary text-secondary-foreground px-2 py-1 text-sm"
              >
                <span className="text-muted-foreground text-xs">{code}</span>
                {region?.name ?? `Регион ${code}`}
                <button
                  type="button"
                  onClick={() => removeRegion(code)}
                  className="rounded-sm opacity-60 hover:opacity-100 focus:outline-none"
                  aria-label={`Убрать ${region?.name}`}
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )
          })}
        </div>
      )}

      <Popover.Root open={open} onOpenChange={setOpen}>
        <Popover.Trigger asChild>
          <button
            type="button"
            role="combobox"
            aria-expanded={open}
            className={cn(
              'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
              value.length === 0 && 'text-muted-foreground'
            )}
          >
            {value.length === 0
              ? 'Выберите регионы…'
              : `Выбрано: ${value.length}`}
            <ChevronsUpDown className="h-4 w-4 opacity-50 shrink-0" />
          </button>
        </Popover.Trigger>

        <Popover.Portal>
          <Popover.Content
            className="z-50 w-[var(--radix-popover-trigger-width)] rounded-md border bg-popover text-popover-foreground shadow-md"
            sideOffset={4}
            align="start"
          >
            <div className="p-2 border-b">
              <input
                autoFocus
                placeholder="Поиск по названию или коду…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
              />
            </div>
            <div className="max-h-60 overflow-y-auto p-1">
              {filtered.length === 0 ? (
                <p className="py-4 text-center text-sm text-muted-foreground">Не найдено</p>
              ) : (
                filtered.map((region) => {
                  const selected = value.includes(region.code)
                  return (
                    <button
                      key={region.code}
                      type="button"
                      onClick={() => toggle(region.code)}
                      className="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent hover:text-accent-foreground"
                    >
                      <Check
                        className={cn('h-4 w-4 shrink-0', selected ? 'opacity-100' : 'opacity-0')}
                      />
                      <span className="text-muted-foreground text-xs w-6 shrink-0">{region.code}</span>
                      {region.name}
                    </button>
                  )
                })
              )}
            </div>
            {value.length > 0 && (
              <div className="border-t p-2">
                <button
                  type="button"
                  onClick={() => onChange([])}
                  className="w-full text-sm text-muted-foreground hover:text-foreground text-center"
                >
                  Сбросить всё
                </button>
              </div>
            )}
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
    </div>
  )
}
