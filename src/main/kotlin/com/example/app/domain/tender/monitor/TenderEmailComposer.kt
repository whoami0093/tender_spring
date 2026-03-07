package com.example.app.domain.tender.monitor

import com.example.app.common.email.EmailMessage
import com.example.app.domain.tender.source.Tender
import com.example.app.domain.tender.subscription.Subscription
import com.example.app.domain.tender.subscription.emailList
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class TenderEmailComposer {
    private val ruLocale = Locale.forLanguageTag("ru")
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale)
    private val priceFormat =
        NumberFormat.getNumberInstance(ruLocale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }

    fun compose(
        subscription: Subscription,
        tenders: List<Tender>,
    ): EmailMessage {
        val label = subscription.label ?: "подписка #${subscription.id}"
        val subject = "[ЗакупкиМонитор] ${tenders.size} новых закупок — «$label»"
        val body = buildHtml(label, tenders)
        return EmailMessage(
            to = subscription.emailList(),
            subject = subject,
            body = body,
            isHtml = true,
        )
    }

    private fun buildHtml(
        label: String,
        tenders: List<Tender>,
    ): String =
        buildString {
            append("<h2>Найдены новые закупки по подписке «$label»</h2>")
            append("<ol>")
            tenders.forEach { tender ->
                append("<li style='margin-bottom:16px'>")
                append("<strong>${tender.objectInfo.escapeHtml()}</strong><br>")
                tender.customerInn?.let { append("Заказчик (ИНН): $it<br>") }
                tender.maxPrice?.let { append("НМЦ: ${formatPrice(it, tender.currency)}<br>") }
                tender.deadline?.let {
                    val date = it.atZone(ZoneId.of("Europe/Moscow")).format(dateFormatter)
                    append("Дедлайн: $date<br>")
                }
                append("<a href='${tender.eisUrl.escapeHtml()}'>Открыть на ЕИС</a>")
                append("</li>")
            }
            append("</ol>")
        }

    private fun formatPrice(
        price: BigDecimal,
        currency: String,
    ): String {
        val symbol = if (currency == "RUB") "₽" else currency
        return "${priceFormat.format(price)} $symbol"
    }

    private fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
