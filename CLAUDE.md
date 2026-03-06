# CLAUDE.md — Spring Kotlin Starter

Этот файл даёт Claude Code контекст о проекте при работе в этой кодовой базе.

## Стек

| Компонент | Технология | Версия |
|-----------|-----------|--------|
| Язык | Kotlin | 2.1.10 |
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.3 |
| База данных | PostgreSQL | 17 |
| Кэш | Redis | 7 |
| Миграции | Flyway | (managed by Spring Boot) |
| Build | Gradle (Kotlin DSL) | latest wrapper |
| Тесты | JUnit 5 + MockK | 1.13.14 |

## Структура проекта

```
src/main/kotlin/com/example/app/
├── Application.kt                  # Точка входа
├── config/
│   └── RedisConfig.kt              # Настройка кэша и RedisTemplate
├── common/
│   └── exception/
│       ├── AppException.kt         # Иерархия исключений
│       └── GlobalExceptionHandler.kt  # @RestControllerAdvice
└── domain/
    └── user/                       # Пример домена (шаблон для новых)
        ├── User.kt                 # JPA entity
        ├── UserRepository.kt       # Spring Data repository
        ├── UserDtos.kt             # Request/Response DTOs + маппинг
        ├── UserService.kt          # Бизнес-логика + кэширование
        └── UserController.kt       # REST endpoints
```

## Команды разработки

```bash
# Запустить инфраструктуру (PostgreSQL + Redis)
docker compose up -d

# Запустить приложение с локальным профилем
./gradlew bootRun --args='--spring.profiles.active=local'

# Запустить тесты
./gradlew test

# Сборка JAR
./gradlew build

# Остановить инфраструктуру
docker compose down
```

## API Endpoints

| Method | URL | Описание |
|--------|-----|---------|
| GET | /api/v1/users | Список всех пользователей |
| GET | /api/v1/users/{id} | Получить по ID (кэшируется в Redis) |
| POST | /api/v1/users | Создать пользователя |
| PUT | /api/v1/users/{id} | Обновить имя |
| DELETE | /api/v1/users/{id} | Удалить |
| GET | /actuator/health | Health check |

## Соглашения по коду

- Пакеты организованы **по доменам**, не по слоям (domain/user/, domain/order/, ...)
- DTOs живут в `*Dtos.kt` рядом с доменом
- Маппинг entity → DTO через extension functions (`fun User.toResponse()`)
- Исключения: кидаем `AppException` subclasses, обрабатываем в `GlobalExceptionHandler`
- Кэширование через Spring `@Cacheable`/`@CacheEvict` — не вручную
- Транзакции: сервисный слой, `@Transactional(readOnly = true)` по умолчанию

## Добавить новый домен

1. Создать папку `src/main/kotlin/com/example/app/domain/<name>/`
2. Скопировать структуру из `domain/user/` как шаблон
3. Добавить Flyway миграцию `V{N}__create_{name}_table.sql`
4. Написать unit тесты в `src/test/kotlin/com/example/app/domain/<name>/`

## Spec-Based Workflow

Все новые фичи разрабатываются через spec-подход:

```
/spec:new <feature-name>     # создать спеку
/spec:requirements           # определить требования
/spec:approve requirements   # одобрить
/spec:design                 # технический дизайн
/spec:approve design
/spec:tasks                  # список задач
/spec:approve tasks
/spec:implement              # реализация
```

Спеки хранятся в `spec/` и коммитятся вместе с кодом.
