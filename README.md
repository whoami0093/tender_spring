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
