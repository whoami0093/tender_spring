# Requirements: Zakupki Monitor

**Spec ID:** 002
**Feature:** zakupki-parser
**Created:** 2026-03-06
**Status:** Draft

---

## Feature Overview

Сервис мониторинга закупок с поддержкой нескольких **источников** (sources).
Пользователь создаёт **подписку** — указывает источник и набор фильтров (регион, ключевые слова, заказчик, диапазон НМЦ).
Сервис периодически опрашивает каждый источник и **отправляет email о новых закупках**.

Архитектура спроектирована под расширение: добавление нового источника = реализация одного интерфейса `TenderSource`, без изменений в core-логике.

**Источники v1:**
- `GOSPLAN_44` — `GET https://v2.gosplan.info/fz44/purchases`
- `GOSPLAN_223` — `GET https://v2.gosplan.info/fz223/purchases`

---

## User Stories

### US-1: Создать подписку с фильтрами
**Как** пользователь,
**я хочу** настроить подписку с источником и нужными фильтрами,
**чтобы** получать email только о релевантных закупках.

**Acceptance Criteria:**
- [ ] `POST /api/v1/subscriptions` принимает `source` (`GOSPLAN_44` | `GOSPLAN_223`), фильтры и список email-получателей
- [ ] Поддерживаются фильтры: `regions[]`, `objectInfo`, `customerInn`, `maxPriceFrom`, `maxPriceTo`
- [ ] Подписка сохраняется в БД со статусом `ACTIVE`
- [ ] Возвращается созданная подписка с `id`

### US-2: Периодическая проверка новых закупок
**Как** система,
**я хочу** регулярно опрашивать API по каждой активной подписке,
**чтобы** вовремя обнаруживать новые тендеры.

**Acceptance Criteria:**
- [ ] Scheduler запускается по расписанию (настраивается, по умолчанию каждые 30 мин)
- [ ] Для каждой активной подписки вызывается `GET /fz44/purchases` с её фильтрами + `published_after` = время последней проверки
- [ ] Результаты дедуплицируются по `purchase_number` (на случай повторного появления)
- [ ] Обновляется `lastCheckedAt` подписки после каждого успешного цикла

### US-3: Email-уведомление о новых тендерах
**Как** пользователь,
**я хочу** получать email когда появляются новые закупки по моей подписке,
**чтобы** не пропустить подходящие тендеры.

**Acceptance Criteria:**
- [ ] Email отправляется на все адреса в `emails` подписки
- [ ] Уведомление содержит: название (`object_info`), НМЦ (`max_price`), заказчик (ИНН), дедлайн приёма заявок (`collecting_finished_at`), ссылка на ЕИС
- [ ] Если новых закупок нет — email не отправляется
- [ ] При нескольких новых закупках — один сводный email, а не по одному на каждую

### US-4: Управление подписками
**Как** пользователь,
**я хочу** просматривать, редактировать и удалять подписки,
**чтобы** контролировать мониторинг.

**Acceptance Criteria:**
- [ ] `GET /api/v1/subscriptions` — список всех подписок
- [ ] `GET /api/v1/subscriptions/{id}` — детали подписки
- [ ] `PUT /api/v1/subscriptions/{id}` — обновить фильтры или список emails
- [ ] `DELETE /api/v1/subscriptions/{id}` — удалить подписку
- [ ] `PATCH /api/v1/subscriptions/{id}/status` — пауза / возобновление (`ACTIVE` / `PAUSED`)

---

## Functional Requirements

### P0 (Must Have)
- **FR-1:** CRUD подписок: `source` + фильтры + `emails[]`
- **FR-2:** Абстракция `TenderSource` — интерфейс с методом `fetch(filters, publishedAfter): List<Tender>`
- **FR-3:** Реализации: `GosplanFz44Source`, `GosplanFz223Source` (оба через `v2.gosplan.info`)
- **FR-4:** `TenderSourceRegistry` — реестр источников по ключу (`GOSPLAN_44`, `GOSPLAN_223`, ...)
- **FR-5:** Дедупликация по `(source, purchaseNumber)` — таблица `seen_tenders`
- **FR-6:** Scheduler: для каждой активной подписки → выбрать source из реестра → fetch → фильтр новых → email
- **FR-7:** Хранить `lastCheckedAt` на подписку, передавать как `published_after`
- **FR-8:** Сводный email о новых закупках (SMTP, переиспользуем `EmailService` из фичи 001)

### P1 (Should Have)
- **FR-9:** Поддержка API-ключа ГосПлан (`zakupki.gosplan.api-key`) через заголовок
- **FR-10:** Retry при ошибке source (2 попытки, задержка 5s), логирование сбоев — не деактивировать подписку
- **FR-11:** `GET /api/v1/subscriptions/{id}/tenders` — история найденных закупок

### P2 (Nice to Have)
- **FR-12:** Кэширование ответа source в Redis (TTL = интервал проверки)
- **FR-13:** Дополнительные фильтры: `purchaseType[]`, `stage[]`, `classifier[]` (ОКПД2/КТРУ)

---

## API Reference (ГосПлан v2)

Общие параметры для 44-ФЗ и 223-ФЗ:

- `region` (int[]) — код региона (77 = Москва, 78 = СПб)
- `object_info` (string) — поиск по наименованию предмета закупки
- `customer` (string) — ИНН заказчика (10 цифр)
- `max_price_ge` / `max_price_le` (number) — диапазон НМЦ
- `published_after` / `published_before` (date) — дата публикации
- `limit` (int, макс. 100) / `skip` (int, макс. 50000) — пагинация

Отличие 223-ФЗ: дедлайн — `submission_close_at` (вместо `collecting_finished_at`), организатор — `placer` (вместо `owner`).

**Endpoints:**
- Тест (без ключа, 10 req/min): `https://v2test.gosplan.info/{fz44|fz223}/purchases`
- Прод (API-ключ с июля 2026): `https://v2.gosplan.info/{fz44|fz223}/purchases`

---

## Non-Functional Requirements

| Требование | Целевое значение |
|------------|-----------------|
| Интервал проверки | Настраивается, по умолчанию 30 мин |
| Rate limit (тест) | 10 req/min → не более 10 подписок за цикл без задержки |
| Надёжность дедупликации | 100% — один тендер не уведомляет дважды |
| Ошибка API | Логируется, подписка не деактивируется, retry x2 |

---

## Constraints & Assumptions

- Используем `published_after` = `lastCheckedAt` как основной механизм получения дельты
- Дополнительная дедупликация по `(source, purchase_number)` на случай race condition
- При первом создании подписки `lastCheckedAt` = `now()` — уведомляем только о будущих
- Email через SMTP (реализация из фичи 001 — переиспользуем `EmailService`)
- API-ключ ГосПлан пока не нужен (до июля 2026 прод бесплатный)
- Новый источник (например, другой сайт) = новый класс, реализующий `TenderSource` + регистрация в `TenderSourceRegistry`; изменений в scheduler, подписках и email не требуется

---

## Out of Scope

- Парсинг HTML zakupki.gov.ru (заменён на API)
- Скачивание документов тендеров
- Авторизованные разделы ЕИС
- ПП РФ 615 (не в первой итерации)
- Push-уведомления, Telegram, webhook
- UI для управления подписками

---

## Success Metrics

- Создана подписка → scheduler нашёл новые тендеры → пришёл email
- Повторный цикл — те же тендеры не уведомляют снова
- Тесты: клиент API (unit с mock), дедупликация (unit), endpoint (integration)
