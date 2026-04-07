# Technical Design: Tender Panel

**Spec:** 009-tender-panel
**Status:** draft
**Date:** 2026-04-07

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   HTTP Client                        │
└─────────────────────┬───────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────┐
│  TenderController  /api/v1/tenders                  │
│  GET list (filter+sort+page) | GET by id | PATCH    │
└─────────────────────┬───────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────┐
│  TenderService                                       │
│  findAll(filter, pageable) | findById | patch        │
└─────────────────────┬───────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────┐
│  TenderRepository                                    │
│  JpaRepository + JpaSpecificationExecutor           │
└─────────────────────┬───────────────────────────────┘
                      │
                ┌─────▼──────┐
                │ PostgreSQL  │
                │  tenders   │
                └────────────┘
```

---

## Package Structure

```
src/main/kotlin/com/example/app/domain/tender/
├── Tender.kt               # JPA entity + TenderStatus enum
├── TenderRepository.kt     # JpaRepository + JpaSpecificationExecutor
├── TenderSpecification.kt  # Specification builder для фильтров
├── TenderDtos.kt           # Request/Response DTOs + маппинг
├── TenderService.kt        # Бизнес-логика
└── TenderController.kt     # REST endpoints

src/main/resources/db/migration/
└── V6__create_tenders_table.sql

src/test/kotlin/com/example/app/domain/tender/
├── TenderServiceTest.kt
└── TenderControllerTest.kt
```

---

## Data Model

### Entity: Tender.kt

```kotlin
@Entity
@Table(name = "tenders")
class Tender(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true, length = 100)
    val purchaseNumber: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val title: String,
    @Column(length = 255)
    val region: String? = null,
    @Column(length = 500)
    val customer: String? = null,
    @Column(length = 12)
    val customerInn: String? = null,
    @Column(precision = 18, scale = 2)
    val amount: BigDecimal? = null,
    @Column(length = 10)
    val currency: String = "RUB",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val status: TenderStatus = TenderStatus.SENT,
    val deadline: Instant? = null,
    val publishedAt: Instant? = null,
    @Column(columnDefinition = "TEXT")
    val eisUrl: String? = null,
    @Column(nullable = false)
    var takenInWork: Boolean = false,
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
)

enum class TenderStatus { SENT, WON, LOST, IN_PROGRESS }
```

### Migration: V6__create_tenders_table.sql

```sql
CREATE TABLE tenders (
    id              BIGSERIAL        PRIMARY KEY,
    purchase_number VARCHAR(100)     NOT NULL,
    title           TEXT             NOT NULL,
    region          VARCHAR(255),
    customer        VARCHAR(500),
    customer_inn    VARCHAR(12),
    amount          NUMERIC(18, 2),
    currency        VARCHAR(10)      NOT NULL DEFAULT 'RUB',
    status          VARCHAR(50)      NOT NULL DEFAULT 'SENT',
    deadline        TIMESTAMPTZ,
    published_at    TIMESTAMPTZ,
    eis_url         TEXT,
    taken_in_work   BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenders_purchase_number UNIQUE (purchase_number)
);

-- Индексы для фильтрации
CREATE INDEX idx_tenders_region         ON tenders (region);
CREATE INDEX idx_tenders_status         ON tenders (status);
CREATE INDEX idx_tenders_taken_in_work  ON tenders (taken_in_work);
CREATE INDEX idx_tenders_deadline       ON tenders (deadline);
CREATE INDEX idx_tenders_created_at     ON tenders (created_at DESC);

-- GIN индекс для частичного поиска по customer (ilike)
CREATE INDEX idx_tenders_customer_lower ON tenders (lower(customer));
```

---

## API Design

### GET /api/v1/tenders

**Query parameters:**

| Параметр       | Тип            | Описание                                      |
|----------------|----------------|-----------------------------------------------|
| `region`       | String         | Точное совпадение по региону                 |
| `customer`     | String         | Частичное совпадение (case-insensitive)       |
| `status`       | List\<String\> | Один или несколько: `SENT,WON`                |
| `takenInWork`  | Boolean        | `true` / `false` (без параметра = все)        |
| `deadlineFrom` | ISO DateTime   | Фильтр по deadline >=                         |
| `deadlineTo`   | ISO DateTime   | Фильтр по deadline <=                         |
| `amountFrom`   | BigDecimal     | Фильтр по amount >=                           |
| `amountTo`     | BigDecimal     | Фильтр по amount <=                           |
| `numberSearch` | String         | Частичное совпадение по purchase_number       |
| `page`         | Int (0-based)  | Номер страницы, default: 0                    |
| `size`         | Int            | Размер страницы, default: 20, max: 100        |
| `sort`         | String         | `createdAt,desc` / `amount,asc` / `deadline,desc` |

**Response: 200 OK**
```json
{
  "content": [
    {
      "id": 1,
      "purchaseNumber": "0123456789012345678",
      "title": "Поставка оборудования",
      "region": "Москва",
      "customer": "ООО Ромашка",
      "customerInn": "7701234567",
      "amount": 1500000.00,
      "currency": "RUB",
      "status": "SENT",
      "deadline": "2026-05-01T00:00:00Z",
      "publishedAt": "2026-04-01T10:00:00Z",
      "eisUrl": "https://zakupki.gov.ru/...",
      "takenInWork": false,
      "createdAt": "2026-04-07T12:00:00Z",
      "updatedAt": "2026-04-07T12:00:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```

### GET /api/v1/tenders/{id}

**Response: 200 OK** — объект тендера (те же поля)
**Response: 404 Not Found** — `NotFoundException`

### PATCH /api/v1/tenders/{id}

**Request body:**
```json
{ "takenInWork": true }
```

**Response: 200 OK** — обновлённый объект тендера
**Response: 404 Not Found** — `NotFoundException`

---

## Key Implementation Decisions

### 1. Фильтрация через JPA Specification

Используем `JpaSpecificationExecutor<Tender>` в репозитории.
`TenderSpecification` строит `Specification<Tender>` из `TenderFilter` data class:

```kotlin
data class TenderFilter(
    val region: String? = null,
    val customer: String? = null,
    val statuses: List<TenderStatus>? = null,
    val takenInWork: Boolean? = null,
    val deadlineFrom: Instant? = null,
    val deadlineTo: Instant? = null,
    val amountFrom: BigDecimal? = null,
    val amountTo: BigDecimal? = null,
    val numberSearch: String? = null,
)
```

Каждый непустой параметр добавляет AND-предикат. Это чисто, расширяемо,
не требует @Query с кучей optional conditions.

### 2. Пагинация через Spring Pageable

Контроллер принимает `@PageableDefault(size = 20) Pageable pageable`.
Сервис возвращает `Page<TenderResponse>`, контроллер оборачивает в `TenderPageResponse`.

Разрешённые поля сортировки захардкожены в контроллере через `PageableHandlerMethodArgumentResolverCustomizer`
или проверяются в сервисе — защита от SQL injection через произвольные поля.

### 3. PATCH только для takenInWork

Отдельный DTO `PatchTenderRequest(val takenInWork: Boolean)` — ничего лишнего.
Endpoint `PATCH /api/v1/tenders/{id}` обновляет только это поле.

### 4. Без кэширования списка

Список тендеров часто меняется (парсер добавляет новые), кэш Redis здесь не нужен.
`@Cacheable` не применяем для `findAll`.

### 5. Расширяемость под пользователей

Поле `assignedUserId: Long? = null` можно добавить миграцией `V7__add_tender_assigned_user.sql`
без изменения логики. Сервис и спецификация готовы к этому через nullable поле.

---

## Allowed Sort Fields

```kotlin
val ALLOWED_SORT_FIELDS = setOf(
    "createdAt", "updatedAt", "deadline", "amount", "customer", "region", "publishedAt"
)
```

Если клиент передаёт неизвестное поле — `BadRequestException`.

---

## Security

- Все три эндпоинта защищены существующей аутентификацией (без изменений в SecurityConfig)
- PATCH принимает только `takenInWork` — нет риска mass assignment
- Сортировка валидируется по allowlist — нет SQL injection через sort param

---

## Test Strategy

### TenderServiceTest (unit, MockK)
- `findAll` с разными комбинациями фильтров → проверяем что Specification строится корректно
- `findById` — найден / не найден (NotFoundException)
- `patch` — найден / не найден, `takenInWork` обновляется

### TenderControllerTest (@WebMvcTest)
- `GET /api/v1/tenders` — 200, pagination, фильтры в query params
- `GET /api/v1/tenders/{id}` — 200 и 404
- `PATCH /api/v1/tenders/{id}` — 200 и 404
- Используем `@WithMockUser` и `.with(csrf())` согласно паттернам проекта

---

## Frontend Design

### Новые файлы

```
frontend/src/
├── api/
│   └── tenders.ts              # getTenders, getTender, patchTender
├── pages/
│   └── tenders/
│       ├── TendersPage.tsx     # главная страница со списком
│       └── TenderFilters.tsx   # панель фильтров
```

### Изменения в существующих файлах

| Файл | Изменение |
|------|-----------|
| `src/api/types.ts` | + `TenderResponse`, `TenderStatus`, `TenderPageResponse`, `TenderFilter` |
| `src/App.tsx` | + route `/tenders → <TendersPage />` |
| `src/components/shared/Navbar.tsx` | + навигационные ссылки (Подписки / Тендеры) |

### Component Tree

```
TendersPage
├── PageHeader ("Тендеры")
├── TenderFilters
│   ├── Input (numberSearch)
│   ├── Input (customer)
│   ├── RegionSelect (region) — переиспользуем существующий
│   ├── Select (status, multi через badges)
│   ├── Select (takenInWork: все / взято / не взято)
│   ├── Input (amountFrom / amountTo)
│   ├── Input (deadlineFrom / deadlineTo)
│   └── Button "Сбросить фильтры"
├── Table (shadcn/ui)
│   ├── колонки: №, Название, Регион, Заказчик, Сумма, Статус, Дедлайн, В работе
│   ├── Switch в колонке "В работе" → PATCH запрос
│   └── Skeleton rows при загрузке
└── Pagination
    ├── кнопки Пред / След
    └── "Страница X из Y, всего N тендеров"
```

### Типы (types.ts)

```typescript
export type TenderStatus = 'SENT' | 'WON' | 'LOST' | 'IN_PROGRESS'

export interface TenderResponse {
  id: number
  purchaseNumber: string
  title: string
  region: string | null
  customer: string | null
  customerInn: string | null
  amount: number | null
  currency: string
  status: TenderStatus
  deadline: string | null
  publishedAt: string | null
  eisUrl: string | null
  takenInWork: boolean
  createdAt: string
  updatedAt: string
}

export interface TenderPageResponse {
  content: TenderResponse[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface TenderFilter {
  region?: string
  customer?: string
  status?: TenderStatus[]
  takenInWork?: boolean
  deadlineFrom?: string
  deadlineTo?: string
  amountFrom?: number
  amountTo?: number
  numberSearch?: string
  page?: number
  size?: number
  sort?: string
}
```

### API client (tenders.ts)

```typescript
// GET /api/v1/tenders?region=...&status=SENT,WON&page=0&size=20&sort=createdAt,desc
export async function getTenders(filter: TenderFilter): Promise<TenderPageResponse>

// GET /api/v1/tenders/:id
export async function getTender(id: number): Promise<TenderResponse>

// PATCH /api/v1/tenders/:id  { takenInWork: boolean }
export async function patchTender(id: number, takenInWork: boolean): Promise<TenderResponse>
```

### UX-решения

- **Фильтры** — контролируемые inputs, debounce 300ms для текстовых полей
- **Сортировка** — клик по заголовку колонки меняет `sort` параметр (toggle asc/desc)
- **Switch "В работе"** — оптимистичное обновление через TanStack Query `onMutate`
- **Пагинация** — server-side, сброс на page=0 при смене фильтра
- **Статус бейдж**: SENT=secondary, WON=success (зелёный), LOST=destructive, IN_PROGRESS=outline

---

## Migration Sequence

```
V5__add_filter_local_keywords.sql  (existing)
V6__create_tenders_table.sql       (new)
```
