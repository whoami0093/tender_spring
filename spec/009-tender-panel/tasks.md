# Tasks: Tender Panel

**Spec:** 009-tender-panel
**Status:** pending

---

## Phase 1 — Backend: Foundation

- [ ] **T-01** Создать Flyway миграцию `V6__create_tenders_table.sql` с таблицей `tenders`, индексами и constraint `uq_tenders_purchase_number`
- [ ] **T-02** Создать JPA entity `Tender.kt` + enum `TenderStatus` в пакете `domain/tender/`
- [ ] **T-03** Создать `TenderRepository.kt` — `JpaRepository<Tender, Long>` + `JpaSpecificationExecutor<Tender>`

## Phase 2 — Backend: Core Logic

- [ ] **T-04** Создать `TenderDtos.kt` — `TenderResponse`, `PatchTenderRequest`, `TenderPageResponse`, extension fun `Tender.toResponse()`
- [ ] **T-05** Создать `TenderFilter.kt` (data class с параметрами фильтрации) + `TenderSpecification.kt` (builder предикатов)
- [ ] **T-06** Создать `TenderService.kt`:
  - `findAll(filter: TenderFilter, pageable: Pageable): TenderPageResponse`
  - `findById(id: Long): TenderResponse`
  - `patch(id: Long, request: PatchTenderRequest): TenderResponse`
- [ ] **T-07** Создать `TenderController.kt`:
  - `GET /api/v1/tenders` с `@RequestParam` фильтрами и `Pageable`
  - `GET /api/v1/tenders/{id}`
  - `PATCH /api/v1/tenders/{id}`
  - Валидация allowlist полей сортировки

## Phase 3 — Backend: Tests

- [ ] **T-08** Написать `TenderServiceTest.kt` (MockK unit tests):
  - `findAll` с фильтрами
  - `findById` — найден / NotFoundException
  - `patch` — успех / NotFoundException
- [ ] **T-09** Написать `TenderControllerTest.kt` (`@WebMvcTest`):
  - `GET /api/v1/tenders` — 200 с пагинацией
  - `GET /api/v1/tenders/{id}` — 200 и 404
  - `PATCH /api/v1/tenders/{id}` — 200 и 404
  - `@WithMockUser` + `.with(csrf())` на PATCH

## Phase 4 — Frontend: API & Types

- [ ] **T-10** Добавить типы в `frontend/src/api/types.ts`: `TenderStatus`, `TenderResponse`, `TenderPageResponse`, `TenderFilter`
- [ ] **T-11** Создать `frontend/src/api/tenders.ts`: `getTenders(filter)`, `getTender(id)`, `patchTender(id, takenInWork)`

## Phase 5 — Frontend: UI

- [ ] **T-12** Обновить `Navbar.tsx` — добавить навигационные ссылки (Подписки / Тендеры) с `NavLink`
- [ ] **T-13** Создать `frontend/src/pages/tenders/TenderFilters.tsx` — панель фильтров (numberSearch, customer, region, status, takenInWork, amountFrom/To, deadlineFrom/To, кнопка сброса)
- [ ] **T-14** Создать `frontend/src/pages/tenders/TendersPage.tsx`:
  - Таблица с колонками: №, Название, Регион, Заказчик, Сумма, Статус (Badge), Дедлайн, В работе (Switch)
  - Server-side пагинация
  - Сортировка по клику на заголовок колонки
  - Skeleton при загрузке, empty state
  - Switch "В работе" → `patchTender` mutation с `invalidateQueries`
- [ ] **T-15** Добавить route `/tenders` в `App.tsx` и redirect с `/` на `/subscriptions` оставить, добавить `/tenders`

## Phase 6 — Финал

- [ ] **T-16** Прогнать `./gradlew ktlintCheck detekt test` — исправить все замечания
- [ ] **T-17** Собрать фронт `npm run build` в `frontend/`, проверить что статика попадает в `src/main/resources/static/admin/`
- [ ] **T-18** Прогнать `./gradlew build` финальная сборка

---

## Dependencies

```
T-01 → T-02 → T-03 → T-05 → T-06 → T-07
                 ↓                     ↓
               T-04                  T-09
                                      ↑
                              T-06 → T-08

T-10 → T-11 → T-13 → T-14 → T-15
                        ↓
                       T-12
```
