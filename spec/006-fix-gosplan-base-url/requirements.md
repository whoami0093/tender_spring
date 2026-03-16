# Requirements: Fix GOSPLAN_BASE_URL undefined scheme error

## Feature Overview

Monitor падает с `URI with undefined scheme` при каждом цикле проверки подписок.
Причина: `GOSPLAN_BASE_URL=` задана как пустая строка в `.env`, Spring Expression
`${GOSPLAN_BASE_URL:https://v2test.gosplan.info}` применяет default только при
**отсутствии** переменной, но не при пустом значении.

## User Stories

**US-1.** Как оператор, я хочу чтобы приложение продолжало работать даже если
`GOSPLAN_BASE_URL` не задан или задан пустой строкой, используя разумный fallback.

**US-2.** Как разработчик, я хочу чтобы неверная конфигурация давала понятную
ошибку при старте, а не в рантайме при каждом запросе.

## Functional Requirements

| ID | Priority | Requirement |
|----|----------|-------------|
| F1 | P0 | Если `GOSPLAN_BASE_URL` пустой — использовать `https://v2test.gosplan.info` |
| F2 | P0 | `GosplanFz44Source` и `GosplanFz223Source` не падают с `IllegalArgumentException` |
| F3 | P1 | При невалидном URL (не пустом, но без схемы) — fail fast при старте приложения |

## Non-Functional Requirements

- Изменение обратно совместимо: правильно заданный `GOSPLAN_BASE_URL` работает как прежде
- Не требует изменений в `.env` на сервере

## Out of Scope

- Реализация реального API клиента gosplan.info
- Ротация API ключей

## Success Metrics

- `Monitor failed for subscription` ERROR в логах исчезает
- Health check перестаёт падать из-за данной ошибки
