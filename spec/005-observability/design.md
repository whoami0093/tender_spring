# Design — Observability (Grafana Monitoring)

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  VPS (Docker)                                                   │
│                                                                 │
│  ┌──────────────────┐   /actuator/prometheus   ┌────────────┐  │
│  │   Spring Boot    │◄──────────────────────── │ Prometheus │  │
│  │   app:8080       │                           │ :9090      │  │
│  │                  │   stdout (JSON logs)      └─────┬──────┘  │
│  │  - Micrometer    │──────────────┐                  │         │
│  │  - AccessLogFilt │              ▼            ┌─────▼──────┐  │
│  └──────────────────┘         ┌────────┐        │  Grafana   │  │
│                               │Promtail│────────► :3000      │  │
│  ┌──────────────────┐         │        │  push  │            │  │
│  │  node-exporter   │         └────────┘        │ Dashboards │  │
│  │  :9100           │──────────────────────────► Alerts     │  │
│  └──────────────────┘   scrape system metrics   └─────┬──────┘  │
│                                                        │         │
│  ┌──────────────────┐                           ┌─────▼──────┐  │
│  │    Loki          │◄──────────────────────────│  (query)   │  │
│  │    :3100         │      Promtail push logs    └────────────┘  │
│  └──────────────────┘                                            │
│                                                                 │
│  ── internal network ───────────────────────────────────────── │
└─────────────────────────────────────────────────────────────────┘

             Grafana Alerts ──► Telegram Bot
```

---

## Technology Stack Decisions

| Компонент | Выбор | Причина |
|-----------|-------|---------|
| Метрики | **Micrometer + Prometheus** | Уже в Spring Boot Actuator; pull-модель проще в обслуживании |
| Системные метрики | **node-exporter** | Стандарт; даёт CPU/RAM/disk из ОС |
| Логи | **Loki + Promtail** | Легкий (~50MB RAM); нет индекса как в ELK |
| Structured logging | **Spring Boot 3.4 native JSON** | Нет лишних зависимостей; `logging.structured.format.console=logstash` |
| Визуализация | **Grafana** | Единый UI для метрик (Prometheus) и логов (Loki) |
| Алерты | **Grafana Alerting** | Встроено в Grafana; не нужен отдельный Alertmanager |
| Access-log | **OncePerRequestFilter** | Полный контроль над форматом и маскировкой |

---

## File Structure

```
monitoring/
├── prometheus/
│   └── prometheus.yml              # scrape configs
├── loki/
│   └── loki-config.yml             # retention, storage
├── promtail/
│   └── promtail-config.yml         # pipeline: parse JSON, add labels
└── grafana/
    ├── datasources/
    │   └── datasources.yml         # Prometheus + Loki auto-provision
    ├── dashboards/
    │   ├── dashboards.yml          # dashboard provider config
    │   ├── infrastructure.json     # CPU/RAM/JVM dashboard
    │   └── api-overview.json       # HTTP metrics dashboard
    └── alerting/
        ├── contactpoints.yml       # Telegram contact point
        └── rules.yml               # alert rules

docker-compose.monitoring.yml       # мониторинг-стек
src/main/kotlin/com/example/app/
└── config/
    └── AccessLogFilter.kt          # новый фильтр
src/main/resources/
├── application.yml                 # добавить actuator + structured logging
└── logback-spring.xml              # JSON encoder для prod профиля
```

---

## Spring Boot Changes

### 1. Новая зависимость в `build.gradle.kts`

```kotlin
// Micrometer Prometheus registry
runtimeOnly("io.micrometer:micrometer-registry-prometheus")
```

### 2. `application.yml` — включить Prometheus endpoint и JSON логи

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, info, metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true   # включает p50/p95/p99
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99

# JSON структурированные логи (только для prod профиля — в application-prod.yml)
logging:
  structured:
    format:
      console: logstash
```

`logstash` формат выводит каждую строку как JSON:
```json
{"@timestamp":"2026-03-10T12:00:00.000Z","level":"INFO","logger":"AccessLogFilter","message":"...","method":"POST","uri":"/api/v1/subscriptions","status":201,"duration_ms":45}
```

### 3. `AccessLogFilter.kt` — кастомный access-log

```kotlin
@Component
class AccessLogFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(AccessLogFilter::class.java)

    // Поля, которые маскируются в query params и JSON body summary
    private val sensitiveFields = setOf("password", "token", "authorization", "secret", "key")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val start = System.currentTimeMillis()
        val wrapped = ContentCachingResponseWrapper(response)

        chain.doFilter(request, wrapped)

        val duration = System.currentTimeMillis() - start
        val uri = request.requestURI
        val query = request.queryString?.let { maskQuery(it) } ?: ""
        val fullUri = if (query.isNotEmpty()) "$uri?$query" else uri

        log.info(
            "method={} uri={} status={} duration_ms={} response_size={}",
            request.method,
            fullUri,
            wrapped.status,
            duration,
            wrapped.contentSize,
        )

        wrapped.copyBodyToResponse()
    }

    // Пропускаем actuator-эндпоинты — не нужен шум в логах
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith("/actuator")

    private fun maskQuery(query: String): String =
        query.split("&").joinToString("&") { param ->
            val (key, value) = param.split("=", limit = 2).let {
                it[0] to (it.getOrElse(1) { "" })
            }
            if (key.lowercase() in sensitiveFields) "$key=***" else "$key=$value"
        }
}
```

**Что логируется:** метод, URI (с query, маска на sensitive params), HTTP status, время в мс, размер ответа в байтах.
**Что НЕ логируется:** тело запроса и ответа (объём + приватность).

---

## Docker Compose: `docker-compose.monitoring.yml`

```yaml
services:
  prometheus:
    image: prom/prometheus:v2.51.2
    restart: unless-stopped
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
    networks: [internal]

  node-exporter:
    image: prom/node-exporter:v1.7.0
    restart: unless-stopped
    pid: host
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    networks: [internal]

  loki:
    image: grafana/loki:2.9.7
    restart: unless-stopped
    volumes:
      - ./monitoring/loki/loki-config.yml:/etc/loki/config.yml:ro
      - loki_data:/loki
    command: -config.file=/etc/loki/config.yml
    networks: [internal]

  promtail:
    image: grafana/promtail:2.9.7
    restart: unless-stopped
    volumes:
      - ./monitoring/promtail/promtail-config.yml:/etc/promtail/config.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    command: -config.file=/etc/promtail/config.yml
    networks: [internal]

  grafana:
    image: grafana/grafana:10.4.2
    restart: unless-stopped
    # Порт не публикуем наружу — доступ только через Caddy по пути /monitoring
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
      GF_SERVER_ROOT_URL: ${APP_BASE_URL}/monitoring
      GF_SERVER_SERVE_FROM_SUB_PATH: "true"
      GF_FEATURE_TOGGLES_ENABLE: alertingSimplifiedRouting
    volumes:
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources:ro
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./monitoring/grafana/alerting:/etc/grafana/provisioning/alerting:ro
      - grafana_data:/var/lib/grafana
    networks: [internal]

networks:
  internal:
    external: true   # присоединяемся к уже существующей сети из docker-compose.prod.yml

volumes:
  prometheus_data:
  loki_data:
  grafana_data:
```

> Сеть `internal` объявлена как `external: true` — мониторинг присоединяется к сети основного стека, поэтому Prometheus может достучаться до `app:8080`.

---

## Prometheus Config

```yaml
# monitoring/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'spring-app'

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
```

---

## Promtail Config

```yaml
# monitoring/promtail/promtail-config.yml
server:
  http_listen_port: 9080

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: docker-containers
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      # Брать только контейнер 'app'
      - source_labels: [__meta_docker_container_name]
        regex: /app
        action: keep
      - source_labels: [__meta_docker_container_name]
        target_label: container
    pipeline_stages:
      # Парсим JSON-логи Spring Boot
      - json:
          expressions:
            level: level
            logger: logger_name
            message: message
            method: method
            uri: uri
            status: status
            duration_ms: duration_ms
      - labels:
          level:
          method:
          status:
```

---

## Grafana Provisioning

### Datasources

```yaml
# monitoring/grafana/datasources/datasources.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true

  - name: Loki
    type: loki
    url: http://loki:3100
```

### Dashboard Provider

```yaml
# monitoring/grafana/dashboards/dashboards.yml
apiVersion: 1
providers:
  - name: default
    folder: App
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards
```

---

## Alerting Design

### Alert Rules (`monitoring/grafana/alerting/rules.yml`)

| Алерт | Условие | Период | Severity |
|-------|---------|--------|----------|
| HighErrorRate | `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(...[5m]) > 0.05` | 5m | critical |
| HighLatencyP95 | `histogram_quantile(0.95, ...) > 2` | 5m | warning |
| HighHeapUsage | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85` | 10m | warning |
| AppDown | `up{job="spring-app"} == 0` | 1m | critical |

### Telegram Contact Point (`monitoring/grafana/alerting/contactpoints.yml`)

```yaml
apiVersion: 1
contactPoints:
  - orgId: 1
    name: telegram
    receivers:
      - uid: telegram-bot
        type: telegram
        settings:
          bottoken: ${TELEGRAM_BOT_TOKEN}
          chatid: ${TELEGRAM_CHAT_ID}
          message: |
            {{ .Status }}: {{ .CommonLabels.alertname }}
            {{ range .Alerts }}{{ .Annotations.summary }}{{ end }}
```

---

## Доступ к Grafana

Grafana доступна по пути `/monitoring` через Caddy — без отдельного порта и поддомена.

**Без домена:** `http://VPS_IP/monitoring`
**С доменом:** `https://yourdomain.com/monitoring` (менять только `APP_BASE_URL` в `.env`)

### Caddyfile

```caddyfile
# Без домена — слушаем на IP (без HTTPS)
:80 {
    handle /monitoring* {
        reverse_proxy grafana:3000
    }
    reverse_proxy app:8080
}

# С доменом — раскомментировать вместо :80
# yourdomain.com {
#     handle /monitoring* {
#         reverse_proxy grafana:3000
#     }
#     reverse_proxy app:8080
# }
```

В `.env` добавить:
```
APP_BASE_URL=http://VPS_IP   # или https://yourdomain.com при наличии домена
```

---

## Security Considerations

- Prometheus, Loki, node-exporter — **не публикуют порты наружу** (только внутренняя сеть Docker)
- Grafana доступна только через Caddy reverse proxy (HTTPS)
- `GRAFANA_ADMIN_PASSWORD` хранится в `.env` и GitHub Secrets
- `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID` — только в `.env`
- `/actuator/prometheus` endpoint закрыт через Spring Security для внешних запросов (только `app` network → разрешен Prometheus внутри Docker)

---

## Performance Considerations

- Prometheus scrape interval = 15s — достаточно для мониторинга, минимальный overhead
- Loki не индексирует содержимое логов (только labels) — потребление CPU/RAM близко к минимуму
- `AccessLogFilter` без буферизации тела запроса/ответа — нет memory overhead
- node-exporter: < 10MB RAM
- Promtail: < 20MB RAM
- Суммарный overhead стека мониторинга: ~200-300MB RAM

---

## Deployment Architecture

```
# Запуск (на VPS)

# 1. Основной стек (уже запущен)
docker compose -f docker-compose.prod.yml up -d

# 2. Мониторинг-стек
docker compose -f docker-compose.monitoring.yml up -d

# Оба стека разделяют сеть 'internal'
# Prometheus видит app:8080 напрямую
```

В CI/CD (GitHub Actions) добавить шаг деплоя мониторинга после основного деплоя.

---

## Technical Risks & Mitigations

| Риск | Вероятность | Митигация |
|------|-------------|-----------|
| Loki/Prometheus съедают RAM на маленьком VPS | Средняя | Retention 15d/7d + limits в loki-config (ingestion rate limit) |
| Promtail не находит логи контейнера | Низкая | Использовать Docker service discovery вместо static file paths |
| Grafana dashboards слетают после обновления | Низкая | Все дашборды как JSON-файлы в репозитории (provisiong) |
| AccessLogFilter логирует чувствительные данные | Средняя | Маска query params по списку ключей + тело не логируется |
| `internal` network не существует при `up` мониторинга | Низкая | `external: true` + инструкция запускать основной стек первым |
