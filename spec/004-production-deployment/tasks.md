# Tasks — Production Deployment

## Overview

Большая часть кода уже реализована в рамках работы по ветке. Оставшиеся задачи — инфраструктурные: настройка VPS, секретов и первый запуск.

| Фаза | Задач | Статус |
|------|-------|--------|
| 1. Код и CI/CD | 6 | ✅ Готово |
| 2. Настройка VPS | 5 | ⬜ Ожидает |
| 3. GitHub Secrets | 3 | ⬜ Ожидает |
| 4. Первый деплой | 4 | ⬜ Ожидает |
| 5. Проверка | 3 | ⬜ Ожидает |

---

## Фаза 1 — Код и CI/CD (выполнено)

- [x] `Dockerfile` — multi-stage build (Node → Gradle → JRE)
- [x] `.dockerignore` — исключить `.git`, `build/`, `node_modules/`
- [x] `docker-compose.prod.yml` — app + postgres + redis + caddy
- [x] `Caddyfile` — reverse proxy с авто-SSL через `{$APP_DOMAIN}`
- [x] `.env.example` — шаблон всех переменных окружения
- [x] `.github/workflows/ci.yml` — job `deploy`: build → push GHCR → SSH deploy

---

## Фаза 2 — Настройка VPS

- [ ] **T-01** Создать VPS на Timeweb Cloud (Ubuntu 22.04, от 1 CPU / 2 GB RAM)
- [ ] **T-02** Установить Docker на VPS
  ```bash
  curl -fsSL https://get.docker.com | sh
  ```
- [ ] **T-03** Создать директорию `/opt/app` на VPS
  ```bash
  mkdir -p /opt/app
  ```
- [ ] **T-04** Скопировать `docker-compose.prod.yml` и `Caddyfile` на VPS
  ```bash
  scp docker-compose.prod.yml Caddyfile root@<VPS_IP>:/opt/app/
  ```
- [ ] **T-05** Создать `.env` на VPS из `.env.example`, заполнить все значения
  ```bash
  nano /opt/app/.env
  ```

_Зависимости: T-02 → T-03 → T-04 → T-05_

---

## Фаза 3 — GitHub Secrets

- [ ] **T-06** Добавить SSH-ключ для деплоя:
  - Сгенерировать пару: `ssh-keygen -t ed25519 -C "github-deploy"`
  - Добавить публичный ключ в `~/.ssh/authorized_keys` на VPS
- [ ] **T-07** Добавить GitHub Secrets в репозиторий (Settings → Secrets → Actions):
  - `VPS_HOST` — IP-адрес VPS
  - `VPS_USER` — пользователь SSH (обычно `root`)
  - `VPS_SSH_KEY` — содержимое приватного ключа
- [ ] **T-08** Сделать GHCR-пакет публичным:
  - GitHub → Packages → `tender_spring` → Package settings → Change visibility → Public

_Зависимости: T-06 → T-07_

---

## Фаза 4 — Первый деплой

- [ ] **T-09** Направить домен на IP VPS (A-запись у регистратора)
- [ ] **T-10** Убедиться что домен прописан в `.env` (`APP_DOMAIN=tender.example.com`)
- [ ] **T-11** Сделать `git push origin main` для запуска pipeline
- [ ] **T-12** Следить за прогрессом в GitHub Actions → убедиться что все три job прошли (lint ✓ test ✓ deploy ✓)

_Зависимости: T-05, T-07, T-08 → T-11_

---

## Фаза 5 — Проверка

- [ ] **T-13** Проверить `/actuator/health` возвращает `UP`
  ```bash
  curl https://<APP_DOMAIN>/actuator/health
  ```
- [ ] **T-14** Открыть `https://<APP_DOMAIN>/admin` — убедиться что панель загружается и SSL валиден
- [ ] **T-15** Сделать тестовое изменение → push → убедиться что автодеплой отработал

---

## Граф зависимостей

```
T-01 (VPS)
  └── T-02 (Docker)
        └── T-03 (mkdir)
              └── T-04 (scp files)
                    └── T-05 (.env)
                          └── T-11 (push → deploy) ◄── T-07 (secrets)
                                                    ◄── T-08 (GHCR public)
                                                    ◄── T-09 (DNS)
                                │
                                ▼
                          T-12 (verify CI)
                                │
                                ▼
                    T-13, T-14, T-15 (smoke tests)
```
