package com.example.app.domain.tender.monitor

import com.example.app.domain.tender.source.Tender
import com.example.app.domain.tender.subscription.Subscription
import com.example.app.domain.tender.subscription.SubscriptionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class TenderEmailComposerTest {

    private val composer = TenderEmailComposer()

    @Test
    fun `subject contains tender count and subscription label`() {
        val sub = buildSubscription(label = "Стройка Москва")
        val tenders = listOf(buildTender("NUM-001"), buildTender("NUM-002"), buildTender("NUM-003"))

        val message = composer.compose(sub, tenders)

        assertThat(message.subject).contains("3")
        assertThat(message.subject).contains("Стройка Москва")
    }

    @Test
    fun `subject uses subscription id when label is null`() {
        val sub = buildSubscription(id = 42L, label = null)

        val message = composer.compose(sub, listOf(buildTender("NUM-001")))

        assertThat(message.subject).contains("42")
    }

    @Test
    fun `body contains all tender names`() {
        val tenders = listOf(
            buildTender("NUM-001", objectInfo = "Закупка мебели"),
            buildTender("NUM-002", objectInfo = "Ремонт дорог"),
        )

        val message = composer.compose(buildSubscription(), tenders)

        assertThat(message.body).contains("Закупка мебели")
        assertThat(message.body).contains("Ремонт дорог")
    }

    @Test
    fun `body contains formatted price with rouble sign`() {
        val tender = buildTender("NUM-001", maxPrice = BigDecimal("1500000.00"))

        val message = composer.compose(buildSubscription(), listOf(tender))

        assertThat(message.body).contains("₽")
        assertThat(message.body).contains("1")  // price digits present
    }

    @Test
    fun `body contains EIS links for each tender`() {
        val tenders = listOf(
            buildTender("NUM-001", eisUrl = "https://zakupki.gov.ru/test/NUM-001"),
            buildTender("NUM-002", eisUrl = "https://zakupki.gov.ru/test/NUM-002"),
        )

        val message = composer.compose(buildSubscription(), tenders)

        assertThat(message.body).contains("https://zakupki.gov.ru/test/NUM-001")
        assertThat(message.body).contains("https://zakupki.gov.ru/test/NUM-002")
    }

    @Test
    fun `email recipients match subscription emails`() {
        val sub = buildSubscription(emails = "[\"a@test.com\",\"b@test.com\"]")

        val message = composer.compose(sub, listOf(buildTender("NUM-001")))

        assertThat(message.to).containsExactlyInAnyOrder("a@test.com", "b@test.com")
    }

    @Test
    fun `isHtml is true`() {
        val message = composer.compose(buildSubscription(), listOf(buildTender("NUM-001")))
        assertThat(message.isHtml).isTrue()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildSubscription(
        id: Long = 1L,
        label: String? = "Test sub",
        emails: String = "[\"user@example.com\"]",
    ) = Subscription(
        id = id,
        source = "GOSPLAN_44",
        label = label,
        emails = emails,
        status = SubscriptionStatus.ACTIVE,
    )

    private fun buildTender(
        purchaseNumber: String,
        objectInfo: String = "Test tender",
        maxPrice: BigDecimal = BigDecimal("100000"),
        eisUrl: String = "https://zakupki.gov.ru/test/$purchaseNumber",
    ) = Tender(
        purchaseNumber = purchaseNumber,
        objectInfo = objectInfo,
        customerInn = "7710538450",
        maxPrice = maxPrice,
        currency = "RUB",
        deadline = Instant.parse("2026-04-01T10:00:00Z"),
        publishedAt = Instant.now(),
        eisUrl = eisUrl,
        source = "GOSPLAN_44",
    )
}
