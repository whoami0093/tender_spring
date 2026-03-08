# Design — Production Deployment

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      GitHub                                  │
│                                                             │
│  push → main ──► Actions: lint → test → build → deploy     │
│                                          │                  │
│                                   ghcr.io/whoami0093/       │
│                                   tender_spring:latest      │
└──────────────────────────────────────────┬──────────────────┘
                                           │ docker pull
                                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Timeweb VPS (Ubuntu 22.04)                 │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Docker Compose (internal network)       │   │
│  │                                                     │   │
│  │  :80/:443                                           │   │
│  │  ┌─────────┐     :8080    ┌─────────────────────┐  │   │
│  │  │  Caddy  │────────────►│   Spring Boot App    │  │   │
│  │  │  (SSL)  │             │   (JRE 21)           │  │   │
│  │  └─────────┘             └──────────┬──────────┘  │   │
│  │                                     │              │   │
│  │                          ┌──────────┴──────────┐   │   │
│  │                          │                     │   │   │
│  │                   ┌──────▼──────┐    ┌─────────▼─┐ │   │
│  │                   │ PostgreSQL  │    │   Redis   │ │   │
│  │                   │     17      │    │     7     │ │   │
│  │                   └──────┬──────┘    └─────────┬─┘ │   │
│  │                          │                     │   │   │
│  │                   [postgres_data]         [redis_data]  │
│  │                   (Docker volume)        (Docker volume) │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## CI/CD Pipeline

```
push to main
     │
     ▼
┌─────────┐     fail
│  lint   ├──────────► ✗ stop
│ktlint   │
│detekt   │
└────┬────┘
     │ pass
     ▼
┌─────────┐     fail
│  test   ├──────────► ✗ stop (upload test reports artifact)
│JUnit 5  │
│Postgres │
│Redis    │
└────┬────┘
     │ pass (only push, not PR)
     ▼
┌──────────────────────────┐
│  deploy                  │
│                          │
│  1. docker buildx build  │
│  2. push → GHCR          │
│  3. SSH → VPS            │
│     docker compose pull  │
│     docker compose up    │  ──► ✓ live
│     docker image prune   │
└──────────────────────────┘
```

---

## Technology Decisions

### Reverse Proxy: Caddy (не nginx)

| Критерий | Caddy | nginx + certbot |
|---------|-------|-----------------|
| SSL | Автоматически (Let's Encrypt) | Ручная настройка certbot |
| Конфиг | 3 строки | ~50 строк + cron для renewal |
| Renewal | Встроен | Отдельный cron/systemd |
| HTTP→HTTPS | Автоматически | Дополнительный server block |

**Решение:** Caddy — меньше конфигурации, меньше точек отказа.

### Registry: GitHub Container Registry (GHCR)

- Бесплатно для публичных репозиториев
- Авторизация через `GITHUB_TOKEN` (не нужны доп. секреты)
- Образ хранится рядом с кодом

### Docker Build: multi-stage (уже в Dockerfile)

```
Stage 1: node:20-alpine    → сборка React frontend
Stage 2: eclipse-temurin:21 → сборка Spring Boot JAR (Gradle)
Stage 3: eclipse-temurin:21-jre → минимальный runtime образ
```

Финальный образ содержит только JRE + JAR — без исходников, Gradle, Node.

---

## Files Layout

```
/                                   (репозиторий)
├── Dockerfile                      # multi-stage build
├── .dockerignore                   # исключения из build context
├── Caddyfile                       # конфиг реверс-прокси
├── docker-compose.yml              # локальная разработка
├── docker-compose.prod.yml         # production
├── .env.example                    # шаблон переменных
└── .github/workflows/ci.yml        # CI/CD pipeline

/opt/app/                           (VPS)
├── docker-compose.prod.yml         # скопировано вручную
├── Caddyfile                       # скопировано вручную
└── .env                            # создаётся вручную (из .env.example)
```

---

## Environment Variables

| Переменная | Где используется | Пример |
|-----------|-----------------|--------|
| `APP_DOMAIN` | Caddy (SSL cert) | `tender.example.com` |
| `DB_NAME` | Postgres + App | `appdb` |
| `DB_USER` | Postgres + App | `appuser` |
| `DB_PASSWORD` | Postgres + App | `str0ng_p@ss` |
| `REDIS_PASSWORD` | Redis + App | `r3dis_p@ss` |
| `ADMIN_USER` | Spring Security | `admin` |
| `ADMIN_PASSWORD` | Spring Security | `s3cur3_p@ss` |
| `SMTP_HOST/PORT/USER/PASS` | Email | `smtp.gmail.com` |
| `EMAIL_FROM` | Email | `noreply@example.com` |
| `GOSPLAN_API_KEY` | Gosplan client | `...` |

**GitHub Secrets (для CI/CD):**
| Secret | Назначение |
|--------|-----------|
| `VPS_HOST` | IP-адрес VPS |
| `VPS_USER` | SSH-пользователь (`root` или `deploy`) |
| `VPS_SSH_KEY` | Приватный SSH-ключ (PEM) |

---

## Network Security

```
Internet
   │
   │ :80, :443
   ▼
┌──────────┐
│  Caddy   │  ← единственный контейнер с публичными портами
└────┬─────┘
     │ internal network only
     ▼
┌──────────┐   ┌──────────┐   ┌──────────┐
│   app    │──►│ postgres │   │  redis   │
│ :8080    │   │ :5432    │   │ :6379    │
└──────────┘   └──────────┘   └──────────┘

Postgres и Redis НЕ имеют проброса портов наружу.
```

---

## Deployment Strategy

**Zero-downtime для БД:** postgres и redis не перезапускаются при деплое новой версии app.

```bash
# Только app перезапускается:
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d --no-deps --force-recreate app
```

**Откат:** вручную через `docker compose up` с предыдущим тегом образа.

---

## Initial VPS Setup (однократно)

```bash
# 1. Установить Docker
curl -fsSL https://get.docker.com | sh

# 2. Создать директорию
mkdir -p /opt/app && cd /opt/app

# 3. Скопировать файлы (с локальной машины)
scp docker-compose.prod.yml Caddyfile root@<IP>:/opt/app/

# 4. Создать .env
cp /etc/skel/.env.example .env  # или вручную
nano .env

# 5. Первый запуск (все сервисы)
docker compose -f docker-compose.prod.yml up -d

# 6. Проверить SSL
curl https://<APP_DOMAIN>/actuator/health
```

---

## Risks & Mitigations

| Риск | Вероятность | Митигация |
|------|------------|-----------|
| Домен не направлен на VPS при первом деплое | Средняя | Caddy продолжит работу, SSL получит позже |
| Секреты попадают в репозиторий | Низкая | `.env` в `.gitignore`, только `.env.example` в git |
| Потеря данных при пересоздании контейнера | Низкая | Named Docker volumes, postgres/redis не пересоздаются |
| SSH-ключ скомпрометирован | Низкая | Отдельный `deploy`-пользователь с правами только на `/opt/app` |
| Образ в GHCR недоступен | Низкая | GHCR имеет высокий SLA; образ хранится бессрочно |
