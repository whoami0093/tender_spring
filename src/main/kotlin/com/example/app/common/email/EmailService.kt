package com.example.app.common.email

import com.example.app.common.exception.EmailSendException
import jakarta.mail.MessagingException
import org.slf4j.LoggerFactory
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val config: EmailConfig,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send(message: EmailMessage) {
        try {
            val mime = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mime, true, "UTF-8")
            helper.setFrom(config.from)
            message.to.forEach { helper.addTo(it) }
            helper.setSubject(message.subject)
            helper.setText(message.body, message.isHtml)
            mailSender.send(mime)
            log.info("Email sent to={} subject=\"{}\"", message.to, message.subject)
        } catch (ex: MailException) {
            log.error("Failed to send email to={}", message.to, ex)
            throw EmailSendException("Failed to send email: ${ex.message}")
        } catch (ex: MessagingException) {
            log.error("Failed to build email to={}", message.to, ex)
            throw EmailSendException("Failed to build email: ${ex.message}")
        }
    }
}
