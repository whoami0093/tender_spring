# Tasks: Frontend Admin Panel

## Overview

| Фаза | Задач | Описание |
|------|-------|---------|
| 1. Backend foundation | 3 | Security, SPA-контроллер, Gradle интеграция |
| 2. Frontend foundation | 4 | Scaffold, роутинг, API-клиент, темизация |
| 3. Core features | 5 | Таблица, создание, редактирование, удаление, toggle |
| 4. Polish | 2 | Toast-уведомления, скелетон/спиннер |
| 5. Deployment | 2 | Dockerfile, документация |
| **Итого** | **16** | |

---

## Phase 1: Backend Foundation

- [x] **1.1** Создать `SecurityConfig.kt` — подключить Spring Security Basic Auth
  - Зависимость `spring-boot-starter-security` в `build.gradle.kts`
  - Все маршруты требуют аутентификации, кроме `/actuator/health`
  - CSRF отключён
  - Credentials из env: `ADMIN_USER` / `ADMIN_PASSWORD`
  - Добавить переменные в `application.yml`:
    ```yaml
    spring:
      security:
        user:
          name: ${ADMIN_USER:admin}
          password: ${ADMIN_PASSWORD:changeme}
    ```

- [x] **1.2** Создать `SpaController.kt` — SPA fallback для React Router
  - `@GetMapping("/admin/**")` → `forward:/admin/index.html`
  - Убедиться, что `/admin/index.html` не входит в петлю (исключить точный путь)

- [x] **1.3** Добавить `frontendBuild` таск в `build.gradle.kts`
  - Таск `Exec`, `workingDir("frontend")`, `commandLine("npm", "run", "build")`
  - Привязать только к `bootJar` (не к `processResources`, не к `test`)
  - Проверить: `./gradlew test` не запускает npm

---

## Phase 2: Frontend Foundation

- [x] **2.1** Инициализировать фронтенд-проект в `frontend/`
  - `npm create vite@latest . -- --template react-ts`
  - Установить зависимости: `tailwindcss`, `@radix-ui/*`, `shadcn/ui` CLI, `react-router-dom`, `@tanstack/react-query`, `react-hook-form`, `zod`, `@hookform/resolvers`, `next-themes`, `lucide-react`
  - Настроить `vite.config.ts`: `base: '/admin/'`, `outDir: '../src/main/resources/static/admin'`
  - Настроить `tailwind.config.ts` и `globals.css` (shadcn/ui CSS-переменные)
  - Инициализировать shadcn/ui: `npx shadcn@latest init`
  - Добавить `frontend/` в `.gitignore` исключения для `node_modules`

- [x] **2.2** Настроить роутинг и базовый Layout
  - `App.tsx`: `BrowserRouter` с `basename="/admin"`
  - `Layout.tsx`: `Navbar` + `<Outlet />`
  - `Navbar.tsx`: логотип/название приложения + `ThemeToggle`
  - Роут `/` → редирект на `/subscriptions`
  - Роут `/subscriptions` → `SubscriptionsPage` (заглушка)

- [x] **2.3** Настроить тёмную/светлую тему
  - Обернуть `App` в `ThemeProvider` (next-themes), `defaultTheme="system"`, `storageKey="admin-theme"`
  - Компонент `ThemeToggle` — кнопка с иконками `Sun` / `Moon` из lucide-react
  - Добавить inline-script в `index.html` против flash of wrong theme:
    ```html
    <script>
      const t = localStorage.getItem('admin-theme')
      if (t === 'dark' || (!t && matchMedia('(prefers-color-scheme: dark)').matches))
        document.documentElement.classList.add('dark')
    </script>
    ```

- [x] **2.4** Создать API-клиент и TypeScript-типы
  - `src/api/types.ts` — интерфейсы `SubscriptionResponse`, `SubscriptionRequest`, `SubscriptionUpdateRequest`, `SubscriptionFilters`
  - `src/api/client.ts` — базовый `apiFetch` wrapper (проверка статуса, JSON parsing, бросает ошибку с сообщением из тела)
  - `src/api/subscriptions.ts` — функции: `getSubscriptions`, `getSubscription`, `createSubscription`, `updateSubscription`, `updateSubscriptionStatus`, `deleteSubscription`
  - Настроить `QueryClient` и `QueryClientProvider` в `main.tsx`

---

## Phase 3: Core Features

*Зависит от: Phase 1 (1.1, 1.2) и Phase 2*

- [x] **3.1** Страница списка подписок (`SubscriptionsPage`)
  - `useQuery` для загрузки списка подписок
  - Таблица shadcn/ui (`Table`, `TableRow`, …) с колонками: Название, Источник, Email-список (truncated), Статус, Последняя проверка, Действия
  - Пустой стейт с иконкой и кнопкой "Создать первую подписку"
  - Кнопка "Новая подписка" в `PageHeader`

- [x] **3.2** Форма создания/редактирования подписки (`SubscriptionDrawer` + `SubscriptionForm`)
  - Drawer (shadcn/ui `Sheet`) открывается по кнопке "Новая подписка" или "Редактировать"
  - `SubscriptionForm` на React Hook Form + Zod:
    - Поле `label` (text input, обязательное)
    - Поле `source` (Select: "gosplan"; disabled при редактировании)
    - Секция "Фильтры":
      - `objectInfo` (text, placeholder "Ключевые слова")
      - `customerInn` (text, placeholder "ИНН заказчика")
      - `maxPriceFrom` / `maxPriceTo` (number inputs)
      - `regions` (multi-select или text input через запятую)
    - Поле `emails` (Textarea, каждый адрес с новой строки или через запятую)
  - Zod-схема: label обязателен, emails — массив валидных email-адресов
  - `useMutation` для создания / обновления, инвалидация кэша после успеха
  - Закрытие Drawer после успешного сохранения

- [x] **3.3** Удаление подписки
  - `AlertDialog` (shadcn/ui) с текстом подтверждения
  - `useMutation` для DELETE, инвалидация кэша
  - Кнопка "Удалить" в строке таблицы (иконка `Trash2`)

- [x] **3.4** Toggle активности подписки
  - `Switch` (shadcn/ui) в строке таблицы
  - `useMutation` для PATCH `/status` (`ACTIVE` ↔ `PAUSED`)
  - Оптимистичное обновление UI или инвалидация кэша
  - Неактивная строка — приглушённый цвет (`opacity-60`)

- [x] **3.5** Поиск по названию в таблице (P1)
  - Input поиска над таблицей
  - Клиентская фильтрация по полю `label` (без доп. запросов к API)

---

## Phase 4: Polish

*Зависит от: Phase 3*

- [x] **4.1** Toast-уведомления (shadcn/ui `Sonner` или `useToast`)
  - Успех: "Подписка создана", "Подписка обновлена", "Подписка удалена"
  - Ошибка: сообщение из тела ответа API или дефолтный текст

- [x] **4.2** Skeleton / Spinner при загрузке
  - `SubscriptionsTable` во время `isLoading` — skeleton-строки (shadcn/ui `Skeleton`)
  - Кнопки в мутациях — disabled + spinner icon пока запрос в процессе

---

## Phase 5: Deployment

*Зависит от: Phase 1–4*

- [x] **5.1** Создать `Dockerfile` в корне репозитория
  - Multi-stage: `node:20-alpine` → `eclipse-temurin:21` → `eclipse-temurin:21-jre`
  - Stage 2: `./gradlew bootJar -x frontendBuild -x test --no-daemon`
  - Проверить: `docker build -t zakupki-app .` завершается успешно
  - Проверить: `docker run -p 8080:8080 -e ADMIN_USER=admin -e ADMIN_PASSWORD=secret ...` открывает `/admin`

- [x] **5.2** Обновить `README.md` / добавить раздел деплоя
  - Инструкция по локальному запуску фронта: `cd frontend && npm install && npm run dev`
  - Переменные окружения: `ADMIN_USER`, `ADMIN_PASSWORD`
  - Команда деплоя через Docker

---

## Task Dependencies

```
1.1 ──────────────────────────────────────► 3.x (API защищён)
1.2 ──────────────────────────────────────► 3.x (SPA работает)
1.3 ──────────────────────────────────────► 5.1 (bootJar собирает фронт)
2.1 ──► 2.2 ──► 2.3
            └──► 2.4 ──► 3.1 ──► 3.2 ──► 3.3
                              └──► 3.4
                              └──► 3.5
                                        └──► 4.1
                                        └──► 4.2 ──► 5.1 ──► 5.2
```
