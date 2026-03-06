# Technical Design: SMTP Email Sending

## Структура файлов

```
src/main/kotlin/com/example/app/common/email/
├── EmailMessage.kt          # data class — входные данные для отправки
├── EmailService.kt          # сервис отправки
└── EmailConfig.kt           # @ConfigurationProperties для SMTP

src/main/kotlin/com/example/app/common/exception/
└── AppException.kt          # добавить EmailSendException

src/test/kotlin/com/example/app/common/email/
└── EmailServiceTest.kt      # unit-тест с MockK

src/main/resources/
└── application.yml          # добавить spring.mail.*

docker-compose.yml           # добавить mailpit для local dev
```

## Классы

### EmailMessage.kt
```kotlin
data class EmailMessage(
    val to: List<String>,
    val subject: String,
    val body: String,
    val isHtml: Boolean = false,
)
```

### EmailConfig.kt
```kotlin
@ConfigurationProperties(prefix = "app.email")
data class EmailConfig(
    val from: String,
)
```

### EmailService.kt
```kotlin
@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val config: EmailConfig,
) {
    fun send(message: EmailMessage) { ... }
}
```
Бросает `EmailSendException` при ошибке отправки.

### AppException.kt — добавить:
```kotlin
class EmailSendException(
    message: String,
    cause: Throwable? = null,
    code: String = "EMAIL_SEND_ERROR",
) : AppException(message, HttpStatus.INTERNAL_SERVER_ERROR, code)
```

## Конфигурация

### application.yml (добавить)
```yaml
spring:
  mail:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:1025}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false

app:
  email:
    from: ${EMAIL_FROM:noreply@example.com}
```

### docker-compose.yml (добавить сервис)
```yaml
mailpit:
  image: axllent/mailpit:latest
  container_name: app-mailpit
  ports:
    - "1025:1025"   # SMTP
    - "8025:8025"   # Web UI → http://localhost:8025
```

## Зависимости (build.gradle.kts)

```kotlin
implementation("org.springframework.boot:spring-boot-starter-mail")
```

## Тестирование

- Unit-тест `EmailServiceTest`: мокаем `JavaMailSender` через MockK, проверяем что `MimeMessage` формируется корректно
- Локальная проверка: Mailpit на `http://localhost:8025` — все письма попадают туда

## Решения

| Вопрос | Решение |
|--------|---------|
| HTML vs plain text | `MimeMessageHelper(msg, true)` — всегда multipart, `setText(body, isHtml)` |
| Несколько получателей | `to: List<String>`, итерируем `helper.addTo()` |
| Ошибки отправки | catch `MailException` → бросаем `EmailSendException` |
| Конфиг `from` | `@ConfigurationProperties("app.email")`, не через `spring.mail` — больше контроля |
