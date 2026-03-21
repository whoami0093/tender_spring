# Tasks: Local Keywords API

## Phase 1 — Backend

- [x] **T1** `SubscriptionDtos.kt`: добавить `localKeywords: List<String> = emptyList()` в `SubscriptionFiltersRequest`
- [x] **T2** `SubscriptionDtos.kt`: в `SubscriptionRequest.toEntity()` сохранять `filterLocalKeywords` из `filters.localKeywords`
- [x] **T3** `SubscriptionDtos.kt`: в `Subscription.toResponse()` включать `localKeywords` из `filterLocalKeywords`
- [x] **T4** `SubscriptionService.kt`: убедиться что метод `update()` обновляет `filterLocalKeywords` (проверить маппинг)

## Phase 2 — Test

- [x] **T5** `TenderLocalKeywordFilterSpec.kt`: обновить `householdGoodsFilters()` до расширенного списка из 27 слов
- [x] **T6** Запустить `./gradlew test` — все тесты зелёные

## Phase 3 — Frontend

- [x] **T7** `frontend/src/api/types.ts`: добавить `localKeywords: string[]` в `SubscriptionFilters`
- [x] **T8** `SubscriptionForm.tsx`: добавить `useState` для `localKeywords` с инициализацией из `existing?.filters.localKeywords ?? []`
- [x] **T9** `SubscriptionForm.tsx`: добавить `TagInput` «Локальные ключевые слова» в блок «Доп. фильтры» с подсказкой
- [x] **T10** `SubscriptionForm.tsx`: включить `localKeywords` в payload `onValid`

## Dependencies

```
T1 → T2 → T3 → T4 → T6
T5 → T6
T7 → T8 → T9 → T10
T1 (types) unblocks T7
```
