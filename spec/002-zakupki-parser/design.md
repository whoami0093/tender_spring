# Design: Zakupki Monitor

**Spec ID:** 002
**Phase:** Design
**Status:** Draft

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP API (REST)                       │
│         SubscriptionController  /api/v1/subscriptions    │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                 SubscriptionService                      │
│           CRUD подписок, валидация source                │
└───────────────────────┬─────────────────────────────────┘
                        │ JPA
┌───────────────────────▼─────────────────────────────────┐
│   PostgreSQL: subscriptions  │  seen_tenders             │
└──────────────────────────────────────────────────────────┘

                   @Scheduled (каждые N мин)
                        │
┌───────────────────────▼─────────────────────────────────┐
│                  MonitorScheduler                        │
│   for each ACTIVE subscription → MonitorService.run()   │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                   MonitorService                         │
│  1. registry.get(source).fetch(filters, lastCheckedAt)  │
│  2. filter seen (by purchaseNumber)                      │
│  3. save new to seen_tenders                             │
│  4. send email if any new                                │
│  5. update lastCheckedAt                                 │
└──────┬──────────────────────────────────────┬───────────┘
       │                                      │
┌──────▼──────────────────┐    ┌─────────────▼───────────┐
│   TenderSourceRegistry  │    │    EmailService (feat001)│
│  GOSPLAN_44 → Fz44Source│    │    TenderEmailComposer   │
│  GOSPLAN_223 → Fz223Src │    └─────────────────────────┘
│  FUTURE_SRC  → ...      │
└──────┬──────────────────┘
       │ RestClient
┌──────▼──────────────────┐
│   v2.gosplan.info API   │
│  GET /fz44/purchases    │
│  GET /fz223/purchases   │
└─────────────────────────┘
```

---

## Domain Structure

```
src/main/kotlin/com/example/app/domain/tender/
├── source/
│   ├── TenderSource.kt              # interface
│   ├── TenderFilters.kt             # data class фильтров подписки
│   ├── Tender.kt                    # нормализованная модель тендера
│   ├── TenderSourceRegistry.kt      # Spring bean, реестр источников
│   └── gosplan/
│       ├── GosplanProperties.kt     # @ConfigurationProperties
│       ├── GosplanClientConfig.kt   # RestClient bean
│       ├── GosplanDtos.kt           # ответ API (raw)
│       ├── GosplanFz44Source.kt     # TenderSource impl
│       └── GosplanFz223Source.kt    # TenderSource impl
├── subscription/
│   ├── Subscription.kt              # @Entity
│   ├── SubscriptionRepository.kt
│   ├── SubscriptionDtos.kt          # request/response + mapping
│   ├── SubscriptionService.kt
│   └── SubscriptionController.kt
└── monitor/
    ├── SeenTender.kt                # @Entity
    ├── SeenTenderRepository.kt
    ├── MonitorService.kt            # бизнес-логика цикла
    ├── MonitorScheduler.kt          # @Scheduled
    └── TenderEmailComposer.kt       # строит EmailMessage
```

---

## Core Abstractions

### TenderSource — интерфейс источника

```kotlin
interface TenderSource {
    val sourceKey: String   // "GOSPLAN_44", "GOSPLAN_223", ...
    fun fetch(filters: TenderFilters, publishedAfter: Instant): List<Tender>
}
```

### TenderFilters — параметры фильтрации

```kotlin
data class TenderFilters(
    val regions: List<Int> = emptyList(),
    val objectInfo: String? = null,      // поиск по названию
    val customerInn: String? = null,     // ИНН заказчика (10 цифр)
    val maxPriceFrom: BigDecimal? = null,
    val maxPriceTo: BigDecimal? = null,
)
```

### Tender — нормализованная модель (общая для всех источников)

```kotlin
data class Tender(
    val purchaseNumber: String,
    val objectInfo: String,
    val customerInn: String?,
    val maxPrice: BigDecimal?,
    val currency: String,
    val deadline: Instant?,          // collecting_finished_at / submission_close_at
    val publishedAt: Instant?,
    val eisUrl: String,              // строится источником
    val source: String,
)
```

### TenderSourceRegistry

```kotlin
@Component
class TenderSourceRegistry(sources: List<TenderSource>) {
    private val registry = sources.associateBy { it.sourceKey }

    fun get(key: String): TenderSource =
        registry[key] ?: throw AppException.NotFound("Unknown source: $key")

    fun keys(): Set<String> = registry.keys
}
```

Добавить новый источник = создать Spring bean, реализующий `TenderSource`. Больше ничего.

---

## Data Model

### Таблица: `subscriptions`

```sql
CREATE TABLE subscriptions (
    id               BIGSERIAL PRIMARY KEY,
    source           VARCHAR(50)  NOT NULL,           -- GOSPLAN_44 | GOSPLAN_223
    label            VARCHAR(255),
    emails           TEXT         NOT NULL,            -- JSON: ["a@b.com","c@d.com"]
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    filter_regions   VARCHAR(255),                     -- "77,78" (comma-sep)
    filter_object_info VARCHAR(500),
    filter_customer_inn VARCHAR(12),
    filter_max_price_from NUMERIC(18,2),
    filter_max_price_to   NUMERIC(18,2),
    last_checked_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_status ON subscriptions (status);
```

### Таблица: `seen_tenders`

```sql
CREATE TABLE seen_tenders (
    id               BIGSERIAL PRIMARY KEY,
    subscription_id  BIGINT       NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    purchase_number  VARCHAR(100) NOT NULL,
    object_info      VARCHAR(500),
    customer_inn     VARCHAR(12),
    max_price        NUMERIC(18,2),
    currency         VARCHAR(10),
    deadline         TIMESTAMPTZ,
    published_at     TIMESTAMPTZ,
    eis_url          TEXT,
    found_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_seen_tenders UNIQUE (subscription_id, purchase_number)
);

CREATE INDEX idx_seen_tenders_subscription ON seen_tenders (subscription_id);
```

### JPA Entities

**Subscription:**
```kotlin
@Entity
@Table(name = "subscriptions")
class Subscription(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false) val source: String,
    @Column var label: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var emails: String,  // JSON
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,

    @Column var filterRegions: String? = null,          // "77,78"
    @Column var filterObjectInfo: String? = null,
    @Column var filterCustomerInn: String? = null,
    @Column var filterMaxPriceFrom: BigDecimal? = null,
    @Column var filterMaxPriceTo: BigDecimal? = null,

    @Column var lastCheckedAt: Instant? = null,
    @Column(nullable = false, updatable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
)

enum class SubscriptionStatus { ACTIVE, PAUSED }
```

**SeenTender:**
```kotlin
@Entity
@Table(name = "seen_tenders")
class SeenTender(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    val subscription: Subscription,

    @Column(nullable = false) val purchaseNumber: String,
    @Column val objectInfo: String? = null,
    @Column val customerInn: String? = null,
    @Column val maxPrice: BigDecimal? = null,
    @Column val currency: String? = null,
    @Column val deadline: Instant? = null,
    @Column val publishedAt: Instant? = null,
    @Column(columnDefinition = "TEXT") val eisUrl: String? = null,
    @Column(nullable = false) val foundAt: Instant = Instant.now(),
)
```

---

## API Design

### Subscriptions CRUD

```
POST   /api/v1/subscriptions                    Создать подписку
GET    /api/v1/subscriptions                    Список всех подписок
GET    /api/v1/subscriptions/{id}               Детали подписки
PUT    /api/v1/subscriptions/{id}               Обновить фильтры / emails
DELETE /api/v1/subscriptions/{id}               Удалить подписку
PATCH  /api/v1/subscriptions/{id}/status        Пауза / возобновление

GET    /api/v1/subscriptions/{id}/tenders       История найденных закупок (P1)
```

### Request / Response DTOs

```kotlin
// POST / PUT body
data class SubscriptionRequest(
    @NotBlank val source: String,                   // "GOSPLAN_44" | "GOSPLAN_223"
    val label: String? = null,
    @NotEmpty val emails: List<@Email String>,
    val filters: SubscriptionFiltersRequest = SubscriptionFiltersRequest(),
)

data class SubscriptionFiltersRequest(
    val regions: List<Int> = emptyList(),
    val objectInfo: String? = null,
    val customerInn: String? = null,
    val maxPriceFrom: BigDecimal? = null,
    val maxPriceTo: BigDecimal? = null,
)

// PATCH /status body
data class SubscriptionStatusRequest(
    @NotNull val status: SubscriptionStatus,
)

// Response
data class SubscriptionResponse(
    val id: Long,
    val source: String,
    val label: String?,
    val emails: List<String>,
    val status: String,
    val filters: SubscriptionFiltersRequest,
    val lastCheckedAt: Instant?,
    val createdAt: Instant,
)
```

---

## HTTP Client (ГосПлан API)

Используем `RestClient` (Spring 6.1, входит в `spring-boot-starter-web`). Новые зависимости не нужны.

```kotlin
// GosplanProperties.kt
@ConfigurationProperties("zakupki.gosplan")
data class GosplanProperties(
    val baseUrl: String = "https://v2test.gosplan.info",
    val apiKey: String? = null,
    val timeoutSeconds: Long = 15,
)

// GosplanClientConfig.kt
@Configuration
@EnableConfigurationProperties(GosplanProperties::class)
class GosplanClientConfig {
    @Bean
    fun gosplanRestClient(props: GosplanProperties): RestClient =
        RestClient.builder()
            .baseUrl(props.baseUrl)
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; ZakupkiMonitor/1.0)")
            .apply { props.apiKey?.let { defaultHeader("X-Api-Key", it) } }
            .build()
}
```

### GosplanFz44Source (пример)

```kotlin
@Component
class GosplanFz44Source(
    private val client: RestClient,
    private val props: GosplanProperties,
) : TenderSource {

    override val sourceKey = "GOSPLAN_44"

    override fun fetch(filters: TenderFilters, publishedAfter: Instant): List<Tender> {
        val response = client.get()
            .uri { builder ->
                builder.path("/fz44/purchases")
                    .queryParamIfPresent("object_info", Optional.ofNullable(filters.objectInfo))
                    .queryParamIfPresent("customer", Optional.ofNullable(filters.customerInn))
                    .queryParamIfPresent("max_price_ge", Optional.ofNullable(filters.maxPriceFrom))
                    .queryParamIfPresent("max_price_le", Optional.ofNullable(filters.maxPriceTo))
                    .also { b -> filters.regions.forEach { b.queryParam("region", it) } }
                    .queryParam("published_after", publishedAfter.toString())
                    .queryParam("limit", 100)
                    .build()
            }
            .retrieve()
            .body<List<GosplanPurchaseDto>>() ?: emptyList()

        return response.map { it.toTender(sourceKey) }
    }
}
```

### GosplanDtos (raw API response)

```kotlin
data class GosplanPurchaseDto(
    @JsonProperty("purchase_number") val purchaseNumber: String,
    @JsonProperty("object_info") val objectInfo: String?,
    @JsonProperty("customers") val customers: List<String>?,
    @JsonProperty("max_price") val maxPrice: BigDecimal?,
    @JsonProperty("currency_code") val currencyCode: String?,
    // 44-ФЗ
    @JsonProperty("collecting_finished_at") val collectingFinishedAt: Instant?,
    // 223-ФЗ
    @JsonProperty("submission_close_at") val submissionCloseAt: Instant?,
    @JsonProperty("published_at") val publishedAt: Instant?,
    @JsonProperty("region") val region: Int?,
)

fun GosplanPurchaseDto.toTender(source: String): Tender {
    val deadline = collectingFinishedAt ?: submissionCloseAt
    val eisUrl = when {
        source == "GOSPLAN_44" ->
            "https://zakupki.gov.ru/epz/order/notice/ea44/view/common-info.html?regNumber=$purchaseNumber"
        else ->
            "https://zakupki.gov.ru/223/purchase/public/purchase/view/info.html?regNumber=$purchaseNumber"
    }
    return Tender(
        purchaseNumber = purchaseNumber,
        objectInfo = objectInfo ?: "",
        customerInn = customers?.firstOrNull(),
        maxPrice = maxPrice,
        currency = currencyCode ?: "RUB",
        deadline = deadline,
        publishedAt = publishedAt,
        eisUrl = eisUrl,
        source = source,
    )
}
```

---

## Monitor Cycle

```kotlin
// MonitorScheduler.kt
@Component
class MonitorScheduler(private val monitorService: MonitorService) {

    @Scheduled(fixedDelayString = "\${zakupki.monitor.interval-minutes:30}m")
    fun run() = monitorService.runCycle()
}

// MonitorService.kt
@Service
@Transactional
class MonitorService(
    private val subscriptionRepository: SubscriptionRepository,
    private val seenTenderRepository: SeenTenderRepository,
    private val registry: TenderSourceRegistry,
    private val emailService: EmailService,
    private val composer: TenderEmailComposer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun runCycle() {
        val subscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE)
        log.info("Monitor cycle started: {} active subscriptions", subscriptions.size)
        subscriptions.forEach { processSubscription(it) }
    }

    fun processSubscription(sub: Subscription) {
        val publishedAfter = sub.lastCheckedAt ?: Instant.now().also { sub.lastCheckedAt = it }
        runCatching {
            val source = registry.get(sub.source)
            val fetched = source.fetch(sub.toFilters(), publishedAfter)
            val knownNumbers = seenTenderRepository
                .findPurchaseNumbersBySubscriptionId(sub.id)
                .toSet()
            val newTenders = fetched.filter { it.purchaseNumber !in knownNumbers }

            if (newTenders.isNotEmpty()) {
                seenTenderRepository.saveAll(newTenders.map { SeenTender.from(sub, it) })
                emailService.send(composer.compose(sub, newTenders))
                log.info("subscription={} new={}", sub.id, newTenders.size)
            }

            sub.lastCheckedAt = Instant.now()
            sub.updatedAt = Instant.now()
        }.onFailure { ex ->
            log.error("Monitor failed for subscription={}", sub.id, ex)
            // подписка остаётся активной, lastCheckedAt не обновляется
        }
    }
}
```

---

## Email Notification

Один сводный email на подписку, HTML-формат.

```
Subject: [ЗакупкиМонитор] 3 новых закупки — «Стройка Москва»

Найдены новые закупки по вашей подписке «Стройка Москва»:

1. Строительство школы в Москве
   Заказчик: 7710538450
   НМЦ: 15 000 000 ₽
   Дедлайн: 25 марта 2026
   Ссылка: https://zakupki.gov.ru/...

2. ...
```

---

## Retry Logic

Retry реализуется в `MonitorService` через `runCatching` + AOP или вручную:

```kotlin
// простой retry без библиотек
fun <T> withRetry(times: Int = 2, delayMs: Long = 5000, block: () -> T): T {
    repeat(times - 1) { attempt ->
        runCatching { return block() }
            .onFailure { Thread.sleep(delayMs) }
    }
    return block()  // последняя попытка — бросает исключение
}
```

---

## Configuration

```yaml
# application.yml (добавить)
zakupki:
  gosplan:
    base-url: ${GOSPLAN_BASE_URL:https://v2test.gosplan.info}
    api-key: ${GOSPLAN_API_KEY:}
    timeout-seconds: 15
  monitor:
    interval-minutes: 30

spring:
  task:
    scheduling:
      enabled: true
```

Также добавить `@EnableScheduling` на `Application.kt`.

---

## Flyway Migrations

```
V2__create_subscriptions_table.sql
V3__create_seen_tenders_table.sql
```

---

## Dependencies (изменения в build.gradle.kts)

Новых зависимостей **не требуется**:
- `RestClient` — в `spring-boot-starter-web` (Spring 6.1 / Boot 3.2+)
- `@Scheduled` — в `spring-boot-starter`
- `EmailService` — уже есть (фича 001)
- Jackson + kotlin module — уже есть

---

## Test Strategy

| Тест | Тип | Что проверяем |
|------|-----|--------------|
| `GosplanFz44SourceTest` | Unit (MockK) | Маппинг DTO → Tender, параметры запроса |
| `GosplanFz223SourceTest` | Unit (MockK) | Маппинг deadline из `submission_close_at` |
| `MonitorServiceTest` | Unit (MockK) | Дедупликация, логика новых, email вызов |
| `TenderSourceRegistryTest` | Unit | Регистрация источников, unknown key = exception |
| `SubscriptionControllerTest` | Integration (MockMvc) | CRUD endpoints, валидация |
| `MonitorIntegrationTest` | Integration | Полный цикл с мок-HTTP сервером |

---

## Technical Risks

| Риск | Вероятность | Митигация |
|------|------------|-----------|
| Изменение структуры API gosplan.info | Средняя | Изолированные DTO + тест на маппинг |
| Rate limit (10 req/min на тесте) | Высокая при > 10 подписок | Задержка между запросами в scheduler |
| Дублирование при рестарте приложения | Низкая | `lastCheckedAt` хранится в БД, дедупликация по `purchase_number` |
| API недоступен | Средняя | Retry x2, лог ошибки, подписка остаётся активной |
