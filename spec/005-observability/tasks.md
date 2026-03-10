# Tasks — Observability (Grafana Monitoring)

## Overview

Реализация разбита на 4 фазы. Каждая фаза завершается рабочим состоянием, которое можно проверить локально или на VPS.

| Фаза | Описание | Результат |
|------|----------|-----------|
| 1. Spring Boot | Метрики + structured logging + access-log | `/actuator/prometheus` отдаёт данные, логи в JSON |
| 2. Monitoring Stack | docker-compose.monitoring.yml + конфиги | Grafana открывается, Prometheus/Loki получают данные |
| 3. Dashboards & Alerts | JSON дашборды + Telegram алерты | Grafana показывает графики, алерты приходят |
| 4. Deployment | Caddy + VPS + CI/CD | `http://VPS_IP/monitoring` работает на проде |

---

## Фаза 1 — Spring Boot Changes

### 1.1 Prometheus метрики

- [x] Добавить зависимость `runtimeOnly("io.micrometer:micrometer-registry-prometheus")` в `build.gradle.kts`
- [x] В `src/main/resources/application.yml` открыть endpoint `prometheus` в actuator:
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
          http.server.requests: true
        percentiles:
          http.server.requests: 0.5, 0.95, 0.99
  ```
- [ ] Проверить локально: `./gradlew bootRun` → `curl http://localhost:8080/actuator/prometheus` возвращает метрики

### 1.2 Structured JSON logging

- [x] Создать `src/main/resources/application-prod.yml` (если не существует) с JSON логами:
  ```yaml
  logging:
    structured:
      format:
        console: logstash
  ```
- [x] Убедиться, что `application-prod.yml` не переопределяет критичные настройки из `application.yml`

### 1.3 AccessLogFilter

- [x] Создать `src/main/kotlin/com/example/app/config/AccessLogFilter.kt`
  - `OncePerRequestFilter` — логирует method, uri, status, duration_ms, response_size
  - Маскирует query params из списка `sensitiveFields`
  - `shouldNotFilter` возвращает `true` для `/actuator/**`
- [x] Написать unit-тест `src/test/kotlin/com/example/app/config/AccessLogFilterTest.kt`:
  - [x] Тест: обычный запрос → лог содержит метод, URI, статус, duration
  - [x] Тест: query param `?password=secret` → в логе `password=***`
  - [x] Тест: запрос на `/actuator/health` → фильтр не вызывается
- [x] Убедиться, что `./gradlew test` проходит

**Зависимости:** нет
**Проверка фазы:** `./gradlew bootRun` → сделать запрос → в stdout виден JSON-лог с полями `method`, `uri`, `status`, `duration_ms`

---

## Фаза 2 — Monitoring Stack

### 2.1 Prometheus config

- [x] Создать `monitoring/prometheus/prometheus.yml`:
  - scrape job `spring-app` → `app:8080/actuator/prometheus`, interval 15s
  - scrape job `node-exporter` → `node-exporter:9100`, interval 15s

### 2.2 Loki config

- [x] Создать `monitoring/loki/loki-config.yml`:
  - filesystem storage (`/loki/chunks`)
  - retention: 7 дней (`retention_period: 168h`)
  - ingestion rate limit: 4MB/s

### 2.3 Promtail config

- [x] Создать `monitoring/promtail/promtail-config.yml`:
  - Docker service discovery (`unix:///var/run/docker.sock`)
  - `relabel_configs`: брать только контейнер с именем `/app`
  - `pipeline_stages`: JSON парсинг полей `level`, `method`, `uri`, `status`, `duration_ms`
  - Labels: `level`, `method`, `status`

### 2.4 Grafana datasources

- [x] Создать `monitoring/grafana/datasources/datasources.yml`:
  - Prometheus: `http://prometheus:9090`, isDefault: true
  - Loki: `http://loki:3100`

### 2.5 docker-compose.monitoring.yml

- [x] Создать `docker-compose.monitoring.yml` с сервисами:
  - `prometheus` (prom/prometheus:v2.51.2), retention 15d
  - `node-exporter` (prom/node-exporter:v1.7.0), pid: host, mount /proc /sys
  - `loki` (grafana/loki:2.9.7)
  - `promtail` (grafana/promtail:2.9.7), mount docker socket
  - `grafana` (grafana/grafana:10.4.2), `GF_SERVER_SERVE_FROM_SUB_PATH=true`, `GF_SERVER_ROOT_URL=${APP_BASE_URL}/monitoring`
- [x] Сеть `internal` объявить как `external: true`
- [x] Добавить `APP_BASE_URL` и `GRAFANA_ADMIN_PASSWORD` в `.env.example`

### 2.6 Caddyfile

- [x] Обновить `Caddyfile`:
  ```caddyfile
  :80 {
      handle /monitoring* {
          reverse_proxy grafana:3000
      }
      handle {
          reverse_proxy app:8080
      }
  }
  ```
- [x] Раскомментировать / добавить Caddy сервис в `docker-compose.prod.yml`

**Зависимости:** Фаза 1 завершена
**Проверка фазы:** `docker compose -f docker-compose.prod.yml -f docker-compose.monitoring.yml up -d` → `http://localhost/monitoring` открывает Grafana, Prometheus в `Configuration > Data Sources` показывает статус `OK`

---

## Фаза 3 — Dashboards & Alerts

### 3.1 Dashboard provider

- [x] Создать `monitoring/grafana/dashboards/dashboards.yml` (Grafana provisioning provider)

### 3.2 Infrastructure dashboard

- [x] Создать `monitoring/grafana/dashboards/infrastructure.json` с панелями:
  - CPU usage %, RAM usage %, Disk usage %
  - JVM heap used vs max, GC pause time, threads

### 3.3 API Overview dashboard

- [x] Создать `monitoring/grafana/dashboards/api-overview.json` с панелями:
  - Stat: RPS, Error Rate, p95 Latency, Total Requests
  - Time series: Requests/s by endpoint, Latency percentiles
  - Table: Top endpoints by avg latency
  - Logs panel: Loki API access logs

### 3.4 Telegram contact point

- [x] Создать `monitoring/grafana/alerting/contactpoints.yml`
- [x] Добавить `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID` в `.env.example`

### 3.5 Alert rules

- [x] Создать `monitoring/grafana/alerting/rules.yml` с правилами:
  - `HighErrorRate`: error rate > 5% за 5 минут → severity=critical
  - `HighLatencyP95`: p95 > 2s за 5 минут → severity=warning
  - `HighHeapUsage`: heap > 85% за 10 минут → severity=warning
  - `AppDown`: `up{job="spring-app"} == 0` за 1 минуту → severity=critical

**Зависимости:** Фаза 2 завершена
**Проверка фазы:** Grafana показывает оба дашборда с живыми данными; в `Alerting > Contact points` Telegram отображается; тестовое уведомление доходит в чат

---

## Фаза 4 — Deployment

### 4.1 VPS подготовка

- [ ] Добавить в `.env` на VPS новые переменные: `APP_BASE_URL`, `GRAFANA_ADMIN_PASSWORD`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`
- [ ] Добавить те же переменные в GitHub Secrets (для CI/CD)

### 4.2 GitHub Actions

- [ ] Добавить в `.github/workflows/deploy.yml` шаг деплоя мониторинга после основного деплоя:
  ```yaml
  - name: Deploy monitoring stack
    run: |
      ssh ${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }} \
        "cd ~/app && docker compose -f docker-compose.monitoring.yml pull && \
         docker compose -f docker-compose.monitoring.yml up -d"
  ```

### 4.3 .gitignore

- [ ] Убедиться, что `.env` в `.gitignore` (не коммитить секреты)
- [ ] Убедиться, что `monitoring/grafana/dashboards/*.json` НЕ в `.gitignore` (дашборды должны быть в репозитории)

### 4.4 Smoke test на проде

- [ ] После деплоя проверить: `http://VPS_IP/monitoring` открывает Grafana
- [ ] Prometheus Data Source — статус `OK`
- [ ] Loki Data Source — статус `OK`
- [ ] На дашборде Infrastructure видны живые данные
- [ ] На дашборде API Overview появляются данные после нескольких запросов к API
- [ ] В Loki Explore: запрос `{container="app"}` возвращает логи с полями `method`, `uri`, `status`
- [ ] Тестовый алерт в Telegram отправляется успешно

**Зависимости:** Фазы 1-3 завершены
**Проверка фазы:** Все smoke tests пройдены; мониторинг работает на проде

---

## Риски и митигация

| Риск | Задача-митигация |
|------|-----------------|
| `internal` сеть не существует при первом `up` мониторинга | Инструкция: всегда запускать `docker-compose.prod.yml` первым |
| Promtail не находит контейнер `app` (имя отличается) | В конфиге использовать `__meta_docker_container_name` regex, проверить реальное имя через `docker ps` |
| Grafana дашборды не загружаются из provisioning | Проверить mount путь в compose и `dashboards.yml` provider config |
| Telegram алерты не приходят | Проверить `bottoken` формат и что бот добавлен в чат как admin |
