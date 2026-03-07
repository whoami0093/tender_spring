# Tasks: Zakupki Monitor

**Spec ID:** 002
**Phase:** Tasks
**Status:** Draft

---

## Overview

Разбивка на 4 фазы, каждая даёт рабочий и тестируемый результат.

```
Фаза 1: Foundation    — БД + абстракции + конфигурация
Фаза 2: Sources       — HTTP-клиент + GosplanFz44 + GosplanFz223
Фаза 3: Subscriptions — CRUD API для подписок
Фаза 4: Monitor       — scheduler + дедупликация + email
Фаза 5: Tests         — unit + integration тесты
```

---

## Фаза 1: Foundation

> Создаём скелет: domain-структуру, core-абстракции, Flyway-миграции, конфигурацию.

- [x] **T-00 Создать пакет `domain/tender/source/` и файлы абстракций:
  - `Tender.kt` — data class нормализованной модели тендера
  - `TenderFilters.kt` — data class фильтров
  - `TenderSource.kt` — interface с `sourceKey` и `fetch()`
  - `TenderSourceRegistry.kt` — Spring компонент, принимающий `List<TenderSource>`

- [x] **T-00 Flyway миграция `V2__create_subscriptions_table.sql`:
  - Таблица `subscriptions` (все колонки по дизайну)
  - Индекс `idx_subscriptions_status`

- [x] **T-00 Flyway миграция `V3__create_seen_tenders_table.sql`:
  - Таблица `seen_tenders` с FK на `subscriptions` (ON DELETE CASCADE)
  - Уникальный констрейнт `(subscription_id, purchase_number)`
  - Индекс `idx_seen_tenders_subscription`

- [x] **T-00 Добавить конфигурацию в `application.yml`:
  ```yaml
  zakupki:
    gosplan:
      base-url: ${GOSPLAN_BASE_URL:https://v2test.gosplan.info}
      api-key: ${GOSPLAN_API_KEY:}
      timeout-seconds: 15
    monitor:
      interval-minutes: 30
  ```

- [x] **T-00 Добавить `@EnableScheduling` в `Application.kt`

---

## Фаза 2: Sources (ГосПлан API)

> HTTP-клиент и два источника. После этой фазы можно вручную вызвать `fetch()` и убедиться что данные приходят.

- [x] **T-00 Создать `GosplanProperties.kt` (`@ConfigurationProperties("zakupki.gosplan")`):
  - поля: `baseUrl`, `apiKey`, `timeoutSeconds`

- [x] **T-00 Создать `GosplanClientConfig.kt`:
  - `@Bean fun gosplanRestClient(props): RestClient`
  - `baseUrl` из properties
  - заголовок `User-Agent: Mozilla/5.0 (compatible; ZakupkiMonitor/1.0)`
  - если `apiKey` не пустой — добавить заголовок `X-Api-Key`
  - `connectTimeout` и `readTimeout` из `timeoutSeconds`

- [x] **T-00 Создать `GosplanDtos.kt`:
  - `GosplanPurchaseDto` (все поля из API: `purchase_number`, `object_info`, `customers`, `max_price`, `currency_code`, `collecting_finished_at`, `submission_close_at`, `published_at`, `region`)
  - extension fun `GosplanPurchaseDto.toTender(source: String): Tender`
  - EIS URL по шаблону в зависимости от source

- [x] **T-00 Создать `GosplanFz44Source.kt`:
  - implements `TenderSource`, `sourceKey = "GOSPLAN_44"`
  - `fetch()`: GET `/fz44/purchases` с параметрами из `TenderFilters` + `published_after` + `limit=100`
  - все фильтры передаются только если не null/empty

- [x] **T-10** Создать `GosplanFz223Source.kt`:
  - implements `TenderSource`, `sourceKey = "GOSPLAN_223"`
  - `fetch()`: GET `/fz223/purchases` — аналогично Fz44Source
  - дедлайн маппится из `submission_close_at`

---

## Фаза 3: Subscriptions CRUD

> REST API для управления подписками. После этой фазы можно создавать/читать подписки.

- [x] **T-11** Создать `Subscription.kt` (`@Entity`, таблица `subscriptions`):
  - все поля по дизайну
  - enum `SubscriptionStatus { ACTIVE, PAUSED }`
  - helper fun `Subscription.toFilters(): TenderFilters`
  - helper fun `Subscription.emailList(): List<String>` (парсит JSON-поле `emails`)

- [x] **T-12** Создать `SubscriptionRepository.kt`:
  - `findAllByStatus(status: SubscriptionStatus): List<Subscription>`

- [x] **T-13** Создать `SubscriptionDtos.kt`:
  - `SubscriptionRequest` с валидацией (`@NotBlank source`, `@NotEmpty emails`, `@Email` на каждом)
  - `SubscriptionFiltersRequest`
  - `SubscriptionStatusRequest`
  - `SubscriptionResponse`
  - extension fun `Subscription.toResponse(): SubscriptionResponse`

- [x] **T-14** Создать `SubscriptionService.kt`:
  - `create(req): SubscriptionResponse` — валидирует source через `TenderSourceRegistry.keys()`
  - `findAll(): List<SubscriptionResponse>`
  - `findById(id): SubscriptionResponse` — бросает `AppException.NotFound` если нет
  - `update(id, req): SubscriptionResponse` — обновляет фильтры и emails
  - `updateStatus(id, status): SubscriptionResponse`
  - `delete(id)` — удаляет (cascade удалит seen_tenders)

- [x] **T-15** Создать `SubscriptionController.kt`:
  - `POST /api/v1/subscriptions`
  - `GET /api/v1/subscriptions`
  - `GET /api/v1/subscriptions/{id}`
  - `PUT /api/v1/subscriptions/{id}`
  - `DELETE /api/v1/subscriptions/{id}` → 204
  - `PATCH /api/v1/subscriptions/{id}/status`

---

## Фаза 4: Monitor

> Ядро системы: scheduler + дедупликация + email. После этой фазы система работает end-to-end.

- [x] **T-16** Создать `SeenTender.kt` (`@Entity`, таблица `seen_tenders`):
  - все поля по дизайну
  - companion fun `SeenTender.from(subscription, tender): SeenTender`

- [x] **T-17** Создать `SeenTenderRepository.kt`:
  - `findPurchaseNumbersBySubscriptionId(subscriptionId: Long): List<String>`
    (нативный запрос: `SELECT purchase_number FROM seen_tenders WHERE subscription_id = ?`)

- [x] **T-18** Создать `TenderEmailComposer.kt`:
  - `compose(subscription, tenders): EmailMessage`
  - subject: `[ЗакупкиМонитор] N новых закупок — «{label}»`
  - body: HTML-список тендеров (название, НМЦ форматированная, дедлайн, ссылка)
  - вспомогательная форматирование: цена с разделителями разрядов + `₽`

- [x] **T-19** Создать `MonitorService.kt`:
  - `runCycle()` — загружает все `ACTIVE` подписки, вызывает `processSubscription` для каждой
  - `processSubscription(sub)`:
    - если `lastCheckedAt == null` — установить `now()` и не продолжать (первый запуск, «бэйслайн»)
    - иначе: fetch → дедупликация → сохранить новые → email если есть → обновить `lastCheckedAt`
    - `runCatching` + `.onFailure { log.error(...) }` — сбой не деактивирует подписку
  - inline `withRetry(times=2, delayMs=5000)` для вызова `source.fetch()`

- [x] **T-20** Создать `MonitorScheduler.kt`:
  - `@Scheduled(fixedDelayString = "\${zakupki.monitor.interval-minutes:30}m")`
  - делегирует в `MonitorService.runCycle()`

---

## Фаза 5: Tests

- [x] **T-21** `TenderSourceRegistryTest` (unit):
  - регистрирует два тестовых источника → `get()` возвращает нужный
  - `get()` с неизвестным ключом → `AppException.NotFound`

- [x] **T-22** `GosplanFz44SourceTest` (unit, MockK):
  - мок `RestClient` → проверить что параметры запроса формируются правильно
  - маппинг `GosplanPurchaseDto.toTender()`: все поля, deadline = `collecting_finished_at`
  - EIS URL содержит `purchase_number`

- [x] **T-23** `GosplanFz223SourceTest` (unit, MockK):
  - deadline маппится из `submission_close_at` (не `collecting_finished_at`)
  - source = `"GOSPLAN_223"` в URL

- [x] **T-24** `MonitorServiceTest` (unit, MockK):
  - новые тендеры (не в `seen_tenders`) → `saveAll` вызван, `emailService.send` вызван
  - все тендеры уже виденные → `saveAll` не вызван, email не отправлен
  - `source.fetch()` бросает исключение → `lastCheckedAt` не обновился, subscriptions остаётся активной
  - первый запуск (`lastCheckedAt = null`) → только устанавливает baseline, email не отправляется

- [x] **T-25** `SubscriptionControllerTest` (integration, MockMvc + H2):
  - `POST` с валидными данными → 201 + корректный response
  - `POST` с невалидным source → 400
  - `POST` с невалидным email → 400
  - `GET /subscriptions` → список
  - `GET /subscriptions/{id}` несуществующий → 404
  - `PATCH /subscriptions/{id}/status` → смена статуса
  - `DELETE /subscriptions/{id}` → 204

- [x] **T-26** `TenderEmailComposerTest` (unit):
  - subject содержит количество тендеров и label подписки
  - body содержит названия, НМЦ, ссылки всех тендеров
  - НМЦ форматируется с разделителем тысяч и `₽`

---

## Зависимости между задачами

```
T-01 (абстракции)
  ├── T-06..T-10 (sources)  →  T-19 (monitor service)
  ├── T-11..T-15 (subscriptions) → T-19
  └── T-16..T-18 (seen, composer) → T-19
          └── T-20 (scheduler)

T-02, T-03 (migrations) → T-11, T-16 (entities)
T-04, T-05 (config) → T-06, T-20
```

---

## Checklist перед завершением

- [ ] `./gradlew test` — все тесты зелёные
- [ ] `./gradlew build` — сборка без ошибок
- [ ] Ручная проверка: создать подписку → запустить `monitorService.runCycle()` вручную через `/actuator` или тест → прийти email
- [ ] Убедиться что повторный запуск не дублирует email
