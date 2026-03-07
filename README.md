# Spring Kotlin Starter

Готовый к использованию шаблон REST API на **Spring Boot 3.4 + Kotlin 2.1 + PostgreSQL + Redis** со встроенным spec-based процессом разработки.

## Быстрый старт

```bash
# 1. Поднять базу данных и Redis
docker compose up -d

# 2. Запустить приложение
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. Проверить что всё работает
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/users
```

## Что включено

- REST API с примером CRUD для `User`
- PostgreSQL через Spring Data JPA + Hibernate
- Миграции через Flyway (`src/main/resources/db/migration/`)
- Redis кэш через Spring Cache (`@Cacheable`)
- Глобальный exception handler с единым форматом ошибок
- Валидация входящих запросов через Bean Validation
- Unit тесты на JUnit 5 + MockK
- Docker Compose для локальной разработки
- Spec-based workflow через `.claude/commands/spec/`

## Стек

| | |
|---|---|
| Kotlin | 2.1.10 |
| Java | 21 |
| Spring Boot | 3.4.3 |
| PostgreSQL | 17 |
| Redis | 7 |

## Тесты

```bash
./gradlew test
```

## Admin Panel

Веб-панель управления подписками доступна по адресу `/admin` (Basic Auth).

```bash
# Локальная разработка фронтенда (hot reload)
cd frontend && npm install && npm run dev
# Vite proxy не нужен — при сборке всё в одном origin

# Переменные окружения для доступа в панель
ADMIN_USER=admin          # default: admin
ADMIN_PASSWORD=changeme   # default: changeme
```

## Деплой через Docker

```bash
docker build -t zakupki-app .
docker run -p 8080:8080 \
  -e DB_HOST=... -e DB_USER=... -e DB_PASSWORD=... \
  -e REDIS_HOST=... \
  -e ADMIN_USER=admin -e ADMIN_PASSWORD=secret \
  -e GOSPLAN_API_KEY=... \
  zakupki-app
```

## Добавление новой фичи (spec-based)

```bash
/spec:new payment-processing
/spec:requirements
/spec:approve requirements
/spec:design
/spec:approve design
/spec:tasks
/spec:approve tasks
/spec:implement
```
