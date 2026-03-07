export const SOURCE_LABELS: Record<string, string> = {
  GOSPLAN_44: 'Госплан 44-ФЗ',
  GOSPLAN_223: 'Госплан 223-ФЗ',
}

export function sourceLabel(key: string): string {
  return SOURCE_LABELS[key] ?? key
}

export const SOURCE_OPTIONS = Object.entries(SOURCE_LABELS).map(([value, label]) => ({
  value,
  label,
}))
