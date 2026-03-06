# Tasks: SMTP Email Sending

## Phase 1 — Foundation

- [x] Добавить зависимость `spring-boot-starter-mail` в `build.gradle.kts`
- [x] Добавить сервис `mailpit` в `docker-compose.yml` (порты 1025 SMTP, 8025 Web UI)
- [x] Добавить конфиг `spring.mail.*` в `application.yml`
- [x] Добавить `app.email.from` в `application.yml`

## Phase 2 — Core

- [x] Создать `EmailMessage.kt` — data class (`to`, `subject`, `body`, `isHtml`)
- [x] Создать `EmailConfig.kt` — `@ConfigurationProperties("app.email")` с полем `from`
- [x] Добавить `@EnableConfigurationProperties(EmailConfig::class)` в `Application.kt`
- [x] Добавить `EmailSendException` в `AppException.kt`
- [x] Создать `EmailService.kt` — метод `send(EmailMessage)` через `JavaMailSender`

## Phase 3 — Testing

- [x] Написать `EmailServiceTest.kt` — мокаем `JavaMailSender` через MockK
- [ ] Проверить локально: запустить `docker compose up -d`, отправить тестовое письмо, открыть `http://localhost:8025`

## Phase 4 — Cleanup

- [x] Убедиться что `spring.mail.properties.mail.smtp.auth=false` для local профиля
- [x] Проверить что `EmailSendException` корректно обрабатывается в `GlobalExceptionHandler`
