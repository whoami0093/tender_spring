# Requirements — Production Deployment

## Feature Overview

Настройка полного цикла доставки приложения на production-сервер: от push в `main` до живого сайта с HTTPS. Включает контейнеризацию, автоматический CI/CD через GitHub Actions и деплой на VPS (Timeweb Cloud).

---

## User Stories

### US-1: Автоматический деплой при push
**Как** разработчик,
**я хочу** чтобы при push в `main` приложение автоматически деплоилось на сервер,
**чтобы** не тратить время на ручной деплой.

**Acceptance criteria:**
- Push в `main` → CI прогоняет lint + tests → собирает Docker-образ → деплоит на VPS
- При падении lint или тестов деплой не происходит
- Весь pipeline виден в GitHub Actions

### US-2: Приложение доступно по HTTPS
**Как** пользователь,
**я хочу** открыть сервис по домену с валидным SSL-сертификатом,
**чтобы** пользоваться безопасным соединением.

**Acceptance criteria:**
- Сайт открывается по `https://domain`
- HTTP → HTTPS редирект работает автоматически
- Сертификат Let's Encrypt обновляется без ручного вмешательства

### US-3: Изоляция prod-конфигурации
**Как** разработчик,
**я хочу** хранить prod-секреты отдельно от кода,
**чтобы** они не попали в репозиторий.

**Acceptance criteria:**
- Секреты (пароли БД, API ключи) хранятся в `.env` на VPS и GitHub Secrets
- `.env` добавлен в `.gitignore`
- В репозитории есть только `.env.example` с описанием всех переменных

---

## Functional Requirements

### P0 — Обязательно

| ID | Требование |
|----|-----------|
| F-01 | Docker-образ собирается через multi-stage build (Node → Gradle → JRE) |
| F-02 | GitHub Actions: job `deploy` запускается только после успешного `test`, только для `push` в `main` |
| F-03 | Docker-образ публикуется в GitHub Container Registry (GHCR) |
| F-04 | На VPS поднимаются: app, postgres, redis, caddy (через `docker-compose.prod.yml`) |
| F-05 | Caddy автоматически получает и обновляет SSL-сертификат Let's Encrypt |
| F-06 | При деплое перезапускается только контейнер `app` (postgres/redis не трогаются) |
| F-07 | Данные postgres и redis сохраняются между перезапусками (Docker volumes) |
| F-08 | Приложение стартует только после того, как postgres и redis прошли healthcheck |

### P1 — Важно

| ID | Требование |
|----|-----------|
| F-09 | Docker build cache через GitHub Actions cache (ускорение сборки) |
| F-10 | Старые Docker-образы удаляются на VPS после деплоя (`docker image prune`) |
| F-11 | `.dockerignore` исключает `.git`, `build/`, `node_modules/` из контекста сборки |
| F-12 | Healthcheck на `/actuator/health` — compose ждёт готовности app |

### P2 — Желательно

| ID | Требование |
|----|-----------|
| F-13 | Уведомление в случае падения деплоя (Telegram / email) |
| F-14 | Скрипт первичной настройки VPS (`setup.sh`) |

---

## Non-Functional Requirements

| Категория | Требование |
|-----------|-----------|
| **Безопасность** | Postgres и Redis не открыты наружу (только внутренняя Docker-сеть) |
| **Безопасность** | SSH-доступ к VPS только по ключу, пароль отключён |
| **Доступность** | `restart: unless-stopped` на всех контейнерах |
| **Производительность** | Холодный старт приложения < 90 секунд |
| **Наблюдаемость** | `/actuator/health` доступен и возвращает статус DB + Redis |

---

## Constraints & Assumptions

- Хостинг: Timeweb Cloud VPS (Ubuntu 22.04 LTS, минимум 1 CPU / 2 GB RAM)
- Реестр образов: GitHub Container Registry (бесплатно для публичных репозиториев)
- Обратный прокси: Caddy 2 (автоматический Let's Encrypt)
- Домен должен быть куплен и направлен на IP VPS до первого деплоя
- Docker и Docker Compose v2 устанавливаются на VPS вручную один раз

---

## Out of Scope

- Kubernetes / оркестрация
- Горизонтальное масштабирование (несколько инстансов app)
- Managed PostgreSQL / Redis (облачные сервисы)
- Blue-green или canary deployment
- Мониторинг (Prometheus/Grafana) — отдельная спека

---

## Success Metrics

- `git push origin main` → приложение обновлено на prod за < 5 минут
- Нулевое время простоя postgres/redis при деплое новой версии app
- HTTPS работает сразу после первого `docker compose up`
