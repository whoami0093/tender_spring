package com.example.app.common.email

import com.example.app.common.exception.EmailSendException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender

class EmailServiceTest {

    private val mailSender = mockk<JavaMailSender>()
    private val config = EmailConfig(from = "noreply@example.com")
    private val service = EmailService(mailSender, config)

    @Test
    fun `send plain text email`() {
        val mime = mockk<MimeMessage>(relaxed = true)
        every { mailSender.createMimeMessage() } returns mime
        every { mailSender.send(mime) } returns Unit

        service.send(EmailMessage(to = listOf("user@example.com"), subject = "Test", body = "Hello"))

        verify(exactly = 1) { mailSender.send(mime) }
    }

    @Test
    fun `send html email`() {
        val mime = mockk<MimeMessage>(relaxed = true)
        every { mailSender.createMimeMessage() } returns mime
        every { mailSender.send(mime) } returns Unit

        service.send(EmailMessage(to = listOf("user@example.com"), subject = "Test", body = "<b>Hello</b>", isHtml = true))

        verify(exactly = 1) { mailSender.send(mime) }
    }

    @Test
    fun `throws EmailSendException on mail failure`() {
        val mime = mockk<MimeMessage>(relaxed = true)
        every { mailSender.createMimeMessage() } returns mime
        every { mailSender.send(mime) } throws MailSendException("SMTP error")

        assertThrows<EmailSendException> {
            service.send(EmailMessage(to = listOf("user@example.com"), subject = "Fail", body = "body"))
        }
    }
}
