# Requirements: SMTP Email Sending

## Контекст

Инфраструктурная фича: сервис отправки email через SMTP. Будет использоваться другими доменами (например, уведомления о тендерах).

## Функциональные требования

### FR-1: Отправка email
- Система умеет отправлять email через SMTP (JavaMailSender)
- Поддерживаемые форматы: plain text и HTML
- Поля письма: `to`, `subject`, `body` (HTML или текст)
- Поддержка нескольких получателей (`to`, `cc`, `bcc`) — опционально

### FR-2: Конфигурация SMTP
- Настройки через `application.yml` / environment variables: host, port, username, password, from-address
- Поддержка TLS/STARTTLS
- Для локальной разработки — Mailpit (SMTP mock в Docker)

### FR-3: API сервиса
- `EmailService` с методом `send(EmailMessage)` — единая точка входа
- `EmailMessage` — data class: to, subject, body, isHtml

## Нефункциональные требования

- Ошибки отправки бросают типизированное исключение (наследник `AppException`)
- Конфигурация через `@ConfigurationProperties`

## Вне скоупа

- Шаблоны (Thymeleaf) — отдельная фича
- Вложения — отдельная фича
- Очередь / retry — отдельная фича
- Парсинг тендеров — отдельная фича
