# Design: Fix GOSPLAN_BASE_URL undefined scheme error

## Root Cause

`application.yml`:
```yaml
zakupki:
  gosplan:
    base-url: ${GOSPLAN_BASE_URL:https://v2test.gosplan.info}
```

Spring `${VAR:default}` — default применяется только если переменная **не задана**.
При `GOSPLAN_BASE_URL=` (пустая строка) Spring биндит `""` в `GosplanProperties.baseUrl`,
`RestClient.baseUrl("")` строит URI без схемы → `IllegalArgumentException`.

## Solution

Фикс в `GosplanClientConfig.kt` — при создании бина добавить проверку:

```kotlin
val baseUrl = props.baseUrl.ifBlank { "https://v2test.gosplan.info" }
RestClient.builder().baseUrl(baseUrl)...
```

### Почему именно здесь

- `GosplanProperties` остаётся чистым data-классом
- Логика fallback сосредоточена в одном месте — там где строится клиент
- Не требует изменений в `application.yml` или `.env`

## Affected Files

| Файл | Изменение |
|------|-----------|
| `GosplanClientConfig.kt` | добавить `.ifBlank { DEFAULT_URL }` |

## Constants

```kotlin
private const val DEFAULT_BASE_URL = "https://v2test.gosplan.info"
```
