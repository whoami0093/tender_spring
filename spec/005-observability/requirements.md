# Requirements — Observability (Grafana Monitoring)

## Feature Overview

Полный стек наблюдаемости для Spring Boot приложения на базе Grafana. Включает:
- **Метрики инфраструктуры** — CPU, RAM, диск, JVM heap, thread pools
- **Метрики API** — latency, throughput, error rate по каждому endpoint
- **Трассировка запросов** — краткая сводка: метод, URL, статус, время ответа, размер тела
- **Алерты** — уведомления при аномалиях (Telegram / email)

Стек: Prometheus (метрики) + Loki (логи) + Grafana (дашборды + алерты) + Promtail (сборщик логов).

---

## User Stories

### US-1: Дашборд ресурсов
**Как** DevOps/разработчик,
**я хочу** видеть в Grafana графики CPU, памяти, диска и JVM,
**чтобы** замечать деградацию ресурсов до того, как упадёт прод.

**Acceptance criteria:**
- Grafana показывает CPU usage (%), RAM usage (%), disk I/O
- JVM: heap used/max, GC pause time, thread count
- Данные обновляются в реальном времени (scrape interval ≤ 15s)

### US-2: Мониторинг API
**Как** разработчик,
**я хочу** видеть статистику по каждому HTTP endpoint,
**чтобы** находить медленные или падающие ручки.

**Acceptance criteria:**
- Grafana показывает: RPS, p50/p95/p99 latency, error rate (4xx/5xx) по endpoint
- Таблица с топ-10 самых медленных запросов за период
- Фильтрация по методу (GET/POST/...) и статус-коду

### US-3: Лог API запросов
**Как** разработчик,
**я хочу** находить в Grafana (Loki) конкретные запросы — что ушло и что вернулось,
**чтобы** дебажить инциденты без SSH на сервер.

**Acceptance criteria:**
- В логах видно: timestamp, method, URL, query params, HTTP status, response time (ms), размер тела ответа
- Request/response body НЕ логируется полностью — только summary (первые 200 символов или ключевые поля)
- Чувствительные данные (пароли, токены) маскируются в логах
- Поиск по URL, статусу, временному диапазону работает в Loki Explore

### US-4: Алерты
**Как** разработчик,
**я хочу** получать уведомление когда что-то идёт не так,
**чтобы** реагировать до того, как пользователи заметят.

**Acceptance criteria:**
- Алерт при error rate > 5% за последние 5 минут
- Алерт при p95 latency > 2s
- Алерт при heap usage > 85%
- Уведомления приходят в Telegram

---

## Functional Requirements

### P0 — Обязательно

| ID | Требование |
|----|-----------|
| F-01 | Prometheus scrape-ит `/actuator/prometheus` каждые 15s |
| F-02 | Micrometer экспортирует стандартные Spring Boot метрики (http.server.requests, jvm.*, system.*) |
| F-03 | Grafana подключена к Prometheus как datasource |
| F-04 | Дашборд "Infrastructure": CPU, RAM, JVM heap, GC, threads |
| F-05 | Дашборд "API Overview": RPS, latency percentiles, error rate по endpoint |
| F-06 | Structured logging в JSON (Logstash encoder или Spring Boot JSON logs) |
| F-07 | Access-log через `CommonsRequestLoggingFilter` или кастомный `OncePerRequestFilter`: метод, URL, статус, время |
| F-08 | Promtail собирает логи контейнера `app` и отправляет в Loki |
| F-09 | Grafana подключена к Loki как datasource |
| F-10 | Все компоненты (Prometheus, Loki, Promtail, Grafana) подняты через `docker-compose.monitoring.yml` |

### P1 — Важно

| ID | Требование |
|----|-----------|
| F-11 | Grafana Alerting: правила для error rate, latency, heap — уведомления в Telegram |
| F-12 | Дашборды provisioned через конфиг (YAML в `monitoring/grafana/dashboards/`) — не кликами в UI |
| F-13 | Datasources provisioned через конфиг (`monitoring/grafana/datasources/`) |
| F-14 | Grafana защищена паролем (admin password через env var) |
| F-15 | Retention: Prometheus хранит данные 15 дней, Loki — 7 дней |

### P2 — Желательно

| ID | Требование |
|----|-----------|
| F-16 | Дашборд "Business Metrics": кастомные счётчики (кол-во созданных пользователей и т.д.) через `MeterRegistry` |
| F-17 | Distributed tracing через Micrometer Tracing + Tempo (Grafana datasource) |
| F-18 | Алерт при недоступности приложения (Prometheus blackbox exporter) |

---

## Non-Functional Requirements

| Категория | Требование |
|-----------|-----------|
| **Безопасность** | Grafana, Prometheus, Loki — только внутри Docker-сети, не открыты наружу напрямую |
| **Безопасность** | Доступ к Grafana через Caddy reverse proxy с базовой аутентификацией |
| **Безопасность** | PII и секреты в логах маскируются (поля: password, token, authorization) |
| **Производительность** | Overhead от метрик/логов < 5% CPU приложения |
| **Надёжность** | Мониторинг стек не влияет на рестарт основного приложения |
| **Объём логов** | Request body не логируется полностью — только первые 200 символов или summary |

---

## Constraints & Assumptions

- VPS уже настроен (spec-004): Docker, Caddy, app/postgres/redis запущены
- Grafana доступна по поддомену (например `monitoring.domain.com`) через Caddy
- Telegram bot token и chat ID для алертов хранятся в `.env` / GitHub Secrets
- Не используем ELK (тяжело для single-node VPS) — Loki значительно легче

---

## Out of Scope

- OpenTelemetry (otel-collector) — отдельная спека при необходимости
- APM (Datadog, New Relic, Elastic APM)
- Полная запись request/response body (приватность + объём)
- Мониторинг других сервисов кроме основного Spring Boot app
- SLA/SLO tracking

---

## Success Metrics

- За 30 минут после деплоя: дашборды показывают живые данные
- Инцидент (error rate spike) виден в Grafana в течение 1 минуты
- Telegram-алерт приходит в течение 2 минут после начала инцидента
- Можно найти конкретный API запрос по временному диапазону и URL в Loki за < 30 секунд
