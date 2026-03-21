# Design: Local Keywords API

## Architecture Overview

```
POST/PUT /api/v1/subscriptions
  body: { filters: { localKeywords: ["хоз", "уборк", ...] } }
         │
         ▼
  SubscriptionFiltersRequest.localKeywords: List<String>
         │  toEntity()
         ▼
  Subscription.filterLocalKeywords = "хоз,уборк,..."   ← DB column (already exists)
         │
         │  toFilters()
         ▼
  TenderFilters.localKeywords: List<String>
         │  matchesTender(tender)
         ▼
  tender.objectInfo.contains(keyword)  →  keep / drop
```

---

## Data Model

Колонка уже существует в таблице `subscriptions` — **миграция не нужна**.

```sql
-- existing column
filter_local_keywords TEXT NULL
-- format: comma-separated substrings, e.g. "хоз,уборк,моющ,чистящ"
```

Формат хранения: CSV без пробелов (`trim()` при чтении), как у `filter_regions` и `filter_object_info`.

---

## Backend Changes

### 1. `SubscriptionDtos.kt`

**`SubscriptionFiltersRequest`** — добавить поле:
```kotlin
data class SubscriptionFiltersRequest(
    val regions: List<Int> = emptyList(),
    val keywords: List<String> = emptyList(),
    val localKeywords: List<String> = emptyList(),   // NEW
    val customerInn: String? = null,
    val maxPriceFrom: BigDecimal? = null,
    val maxPriceTo: BigDecimal? = null,
)
```

**`SubscriptionRequest.toEntity()`** — сохранять в сущность:
```kotlin
filterLocalKeywords = filters.localKeywords
    .takeIf { it.isNotEmpty() }
    ?.joinToString(","),
```

**`Subscription.toResponse()`** — возвращать в ответе:
```kotlin
filters = SubscriptionFiltersRequest(
    ...
    localKeywords = filterLocalKeywords
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList(),
)
```

### 2. `SubscriptionService.kt`

`update()` уже вызывает `req.filters` → маппинг подхватит автоматически,
нужно убедиться что `filterLocalKeywords` обновляется (проверить `update` метод).

---

## Frontend Changes

### 1. `api/types.ts`

```typescript
export interface SubscriptionFilters {
  regions: number[]
  keywords: string[]
  localKeywords: string[]   // NEW
  customerInn?: string
  maxPriceFrom?: number
  maxPriceTo?: number
}
```

### 2. `SubscriptionForm.tsx`

Добавить состояние:
```typescript
const [localKeywords, setLocalKeywords] = useState<string[]>(
  existing?.filters.localKeywords ?? []
)
```

Включить в payload (`onValid`):
```typescript
filters: {
  ...
  localKeywords,
}
```

Разместить TagInput в блоке «Дополнительные фильтры» после регионов:
```
┌─ Дополнительные фильтры ──────────────────────────────┐
│                                                        │
│  Регионы          [multi-select]                       │
│                                                        │
│  Локальные ключи  ← NEW                                │
│  Подсказка: «Вторичный фильтр по названию тендера.     │
│  Тендер попадёт в рассылку только если его название    │
│  содержит хотя бы одно из этих слов»                   │
│  [хоз ×] [уборк ×] [моющ ×]  [+ добавить]             │
│                                                        │
│  ИНН заказчика    [__________]                         │
│  Сумма от / до    [______] [______]                    │
└────────────────────────────────────────────────────────┘
```

### 3. `TenderLocalKeywordFilterSpec.kt` — `householdGoodsFilters()`

Расширенный список 27 слов:

```kotlin
localKeywords = listOf(
    "хоз",         // хозяйственный, хозтовары
    "уборк",       // уборка, уборки
    "убороч",      // уборочный инвентарь
    "клининг",     // клининг
    "моющ",        // моющие средства
    "чистящ",      // чистящие средства
    "дезинф",      // дезинфекция, дезинфицирующий
    "антисепт",    // антисептик
    "гигиен",      // гигиенические средства
    "санит",       // санитарный, санитайзер
    "бытов",       // бытовая/бытовой химия
    "стиральн",    // стиральный порошок
    "мыл",         // мыло жидкое
    "перчатк",     // перчатки резиновые/нитриловые
    "салфетк",     // влажные/дезинфицирующие салфетки
    "швабр",       // швабры
    "щётк",        // щётки для уборки
    "ведр",        // вёдра
    "тряпк",       // тряпки
    "губк",        // губки для мытья
    "ветошь",      // ветошь протирочная
    "полотенц",    // бумажные полотенца
    "туалетн",     // туалетная бумага
    "диспенсер",   // диспенсеры
    "освежит",     // освежитель воздуха
    "для мусора",  // мешки/пакеты для мусора
    "мусорн",      // мусорные мешки/контейнеры
)
```

---

## API Contract

**Новый формат `SubscriptionFiltersRequest`:**
```json
{
  "regions": [54],
  "keywords": ["хоз товары"],
  "localKeywords": ["хоз", "уборк", "моющ", "чистящ"],
  "customerInn": null,
  "maxPriceFrom": null,
  "maxPriceTo": null
}
```

**Backward compatibility:** поле `localKeywords` имеет default `emptyList()` —
существующие клиенты, не передающие его, продолжают работать без изменений.

---

## Files to Change

| Файл | Изменение |
|------|-----------|
| `src/main/kotlin/.../subscription/SubscriptionDtos.kt` | +`localKeywords` в DTO и маппинг |
| `src/main/kotlin/.../subscription/SubscriptionService.kt` | убедиться что update сохраняет поле |
| `frontend/src/api/types.ts` | +`localKeywords: string[]` |
| `frontend/src/pages/subscriptions/SubscriptionForm.tsx` | +state, +TagInput, +payload |
| `src/test/kotlin/.../TenderLocalKeywordFilterSpec.kt` | обновить `householdGoodsFilters()` |

---

## Technical Risks

| Риск | Митигация |
|------|-----------|
| Слово-подстрока слишком короткое (напр. "мыл") → ложные срабатывания | Документировать; в будущем можно добавить min-length валидацию |
| Фронт не передаёт поле → теряется при обновлении | default `emptyList()` на DTO — не потеряется, просто очистится |
