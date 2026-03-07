# Design: Frontend Admin Panel

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  Browser (owner)                    │
│                                                     │
│  React SPA  ──── fetch /api/v1/* ────┐              │
│  (Vite)          Basic Auth header   │              │
└──────────────────────────────────────┼──────────────┘
                                       │ HTTP
┌──────────────────────────────────────▼──────────────┐
│              Spring Boot :8080                      │
│                                                     │
│  /admin/**  → static/admin/index.html  (SPA shell)  │
│  /api/v1/**  → REST controllers                     │
│  Spring Security — Basic Auth for all routes        │
└─────────────────────────────────────────────────────┘
```

Фронт собирается командой `npm run build` в папку
`src/main/resources/static/admin/` и раздаётся Spring Boot как статика.
Один origin → нет CORS, Basic Auth браузер отправляет автоматически.

---

## Technology Stack

| Слой | Технология | Версия |
|------|-----------|--------|
| Bundler | Vite | 5.x |
| UI framework | React | 18.x |
| Язык | TypeScript | 5.x |
| UI-компоненты | shadcn/ui (Radix UI + Tailwind CSS) | latest |
| Роутинг | React Router | v6 |
| Server state | TanStack Query (React Query) | v5 |
| Формы | React Hook Form + Zod | latest |
| Темизация | next-themes | latest |
| Иконки | lucide-react | latest |

**Структура проекта:**
```
frontend/
├── src/
│   ├── api/           # fetch-клиент + типы из SubscriptionResponse
│   ├── components/
│   │   ├── ui/        # shadcn/ui-компоненты (button, table, dialog…)
│   │   └── shared/    # Navbar, ThemeToggle, PageHeader
│   ├── pages/
│   │   └── subscriptions/
│   │       ├── SubscriptionsPage.tsx
│   │       ├── SubscriptionForm.tsx
│   │       └── SubscriptionRow.tsx
│   ├── hooks/         # useSubscriptions, useCreateSubscription, …
│   ├── lib/
│   │   └── utils.ts   # cn(), formatDate()
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── vite.config.ts     # base: '/admin/'
├── tailwind.config.ts
├── tsconfig.json
└── package.json
```

---

## Component Tree

```
App
└── ThemeProvider (next-themes)
    └── Router
        └── Layout
            ├── Navbar
            │   ├── Logo / App name
            │   └── ThemeToggle
            └── Routes
                └── /subscriptions  →  SubscriptionsPage
                    ├── PageHeader ("Подписки") + Button "Новая подписка"
                    ├── SubscriptionsTable
                    │   └── SubscriptionRow × N
                    │       ├── Toggle (active/paused)
                    │       ├── EditButton → SubscriptionDrawer (edit)
                    │       └── DeleteButton → DeleteConfirmDialog
                    └── SubscriptionDrawer (create / edit)
                        └── SubscriptionForm
                            ├── Field: label
                            ├── Field: source (Select, disabled on edit)
                            ├── FiltersSection
                            │   ├── objectInfo
                            │   ├── customerInn
                            │   ├── maxPriceFrom / maxPriceTo
                            │   └── regions (multi-select)
                            └── Field: emails (textarea)
```

---

## API Contract (existing backend)

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| GET | `/api/v1/subscriptions` | — | `SubscriptionResponse[]` |
| GET | `/api/v1/subscriptions/{id}` | — | `SubscriptionResponse` |
| POST | `/api/v1/subscriptions` | `SubscriptionRequest` | `SubscriptionResponse` |
| PUT | `/api/v1/subscriptions/{id}` | `SubscriptionUpdateRequest` | `SubscriptionResponse` |
| PATCH | `/api/v1/subscriptions/{id}/status` | `{status: "ACTIVE"\|"PAUSED"}` | `SubscriptionResponse` |
| DELETE | `/api/v1/subscriptions/{id}` | — | 204 |

**TypeScript-типы (из SubscriptionDtos.kt):**
```ts
interface SubscriptionFilters {
  regions: number[]
  objectInfo?: string
  customerInn?: string
  maxPriceFrom?: number
  maxPriceTo?: number
}

interface SubscriptionResponse {
  id: number
  source: string
  label?: string
  emails: string[]
  status: 'ACTIVE' | 'PAUSED'
  filters: SubscriptionFilters
  lastCheckedAt?: string   // ISO-8601
  createdAt: string        // ISO-8601
}

interface SubscriptionRequest {
  source: string
  label?: string
  emails: string[]
  filters: SubscriptionFilters
}

interface SubscriptionUpdateRequest {
  label?: string
  emails: string[]
  filters: SubscriptionFilters
}
```

---

## Security

### Basic Auth Flow
```
Browser                Spring Boot
   │                       │
   │── GET /admin ─────────►│
   │◄── 401 WWW-Authenticate: Basic ──│
   │                       │
   │── GET /admin ──────────►│
   │   Authorization: Basic base64(user:pass)
   │◄── 200 index.html ────│
   │                       │
   │── GET /api/v1/subscriptions ───►│
   │   (браузер автоматически добавляет заголовок)
   │◄── 200 [...] ─────────│
```

**Spring Security config (новый файл `SecurityConfig.kt`):**
- Все маршруты требуют аутентификации
- Один in-memory пользователь из `application.yml`:
  ```yaml
  spring:
    security:
      user:
        name: ${ADMIN_USER:admin}
        password: ${ADMIN_PASSWORD:changeme}
  ```
- CSRF отключён (SPA на том же origin, Basic Auth stateless)
- `/actuator/health` — публичный (для docker healthcheck)

---

## Theming

Tailwind CSS + shadcn/ui используют CSS-переменные для цветов.
`next-themes` переключает класс `dark` на `<html>`, что автоматически активирует dark-палитру shadcn/ui.

```
localStorage.theme = 'light' | 'dark' | 'system'
                                 ↓
<html class="dark">  ←  next-themes
                                 ↓
CSS variables:  --background, --foreground, --primary …  (shadcn/ui)
```

---

## Build & Deployment

### Gradle интеграция

`frontendBuild` привязан только к `bootJar`, **не** к `processResources` —
`./gradlew test` и `./gradlew compileKotlin` работают без Node.js и без фронтенд-сборки.

```kotlin
val frontendBuild = tasks.register<Exec>("frontendBuild") {
    workingDir("frontend")
    commandLine("npm", "run", "build")
}
// Только bootJar тянет фронт — тесты не затронуты
tasks.named("bootJar") {
    dependsOn(frontendBuild)
}
```

| Команда | Собирает фронт? |
|---------|----------------|
| `./gradlew test` | Нет |
| `./gradlew compileKotlin` | Нет |
| `./gradlew bootJar` | Да |
| `./gradlew build` | Да (включает bootJar) |
| `docker build` | Да (multi-stage) |

### Vite конфиг

```ts
export default defineConfig({
  base: '/admin/',
  build: {
    outDir: '../src/main/resources/static/admin',
    emptyOutDir: true,
  },
})
```

### Spring Boot — SPA fallback

```kotlin
@Controller
class SpaController {
    @GetMapping("/admin/**")
    fun spa() = "forward:/admin/index.html"
}
```

---

## Deployment (Docker)

Multi-stage `Dockerfile` в корне репозитория — единственное, что нужно для деплоя:

```
┌─────────────────────────────────────────────┐
│  Stage 1: node:20-alpine  (frontend build)  │
│  COPY frontend/ → npm ci → npm run build    │
│  OUT: /app/static/admin/**                  │
└─────────────────────────┬───────────────────┘
                          │ COPY
┌─────────────────────────▼───────────────────┐
│  Stage 2: eclipse-temurin:21  (JVM build)   │
│  COPY . → COPY --from=stage1 static files   │
│  ./gradlew bootJar -x frontendBuild         │
│  OUT: app.jar                               │
└─────────────────────────┬───────────────────┘
                          │ COPY
┌─────────────────────────▼───────────────────┐
│  Stage 3: eclipse-temurin:21-jre (runtime)  │
│  ENTRYPOINT java -jar app.jar               │
│  EXPOSE 8080                                │
└─────────────────────────────────────────────┘
```

```dockerfile
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build   # → dist/ = static/admin/

FROM eclipse-temurin:21 AS builder
WORKDIR /app
COPY . .
COPY --from=frontend /app/frontend/dist src/main/resources/static/admin
RUN ./gradlew bootJar -x frontendBuild -x test --no-daemon

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Деплой в одну команду:**
```bash
docker build -t zakupki-app .
docker run -p 8080:8080 \
  -e DB_HOST=... -e DB_PASSWORD=... \
  -e ADMIN_USER=admin -e ADMIN_PASSWORD=secret \
  zakupki-app
```

Нет Node.js, нет Gradle, нет JDK на сервере — только Docker.

---

## Performance

- TanStack Query кэширует ответы API, повторные рендеры не вызывают лишних запросов
- Список подписок небольшой (< 100), виртуализация не нужна
- Vite по умолчанию делает code-splitting и tree-shaking
- shadcn/ui — только копируемые компоненты, в бандл попадает только то, что импортировано

---

## Technical Risks & Mitigations

| Риск | Вероятность | Митигация |
|------|-------------|-----------|
| Браузер не запоминает Basic Auth при навигации по SPA | Низкая | Браузеры держат credentials до закрытия вкладки; при 401 — reload |
| Vite build не запускается в CI (нет Node.js) | Средняя | Добавить в `Dockerfile` и `README` шаг `npm ci` |
| Gradle frontendBuild замедляет `./gradlew test` | Низкая | Привязан к `bootJar`, а не к `processResources` — тесты не затронуты |
| `next-themes` flash of wrong theme при загрузке | Средняя | Добавить inline script в `index.html` для чтения localStorage до рендера |
