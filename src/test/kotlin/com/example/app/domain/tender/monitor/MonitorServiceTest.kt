package com.example.app.domain.tender.monitor

import com.example.app.common.email.EmailService
import com.example.app.domain.tender.source.Tender
import com.example.app.domain.tender.source.TenderFilters
import com.example.app.domain.tender.source.TenderSource
import com.example.app.domain.tender.source.TenderSourceRegistry
import com.example.app.domain.tender.subscription.Subscription
import com.example.app.domain.tender.subscription.SubscriptionRepository
import com.example.app.domain.tender.subscription.SubscriptionStatus
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class MonitorServiceTest {

    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val seenTenderRepository = mockk<SeenTenderRepository>()
    private val registry = mockk<TenderSourceRegistry>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val composer = mockk<TenderEmailComposer>()

    private val service = MonitorService(
        subscriptionRepository, seenTenderRepository, registry, emailService, composer,
    )

    private val tenderSource = mockk<TenderSource>()

    @BeforeEach
    fun setUp() {
        every { registry.get(any()) } returns tenderSource
        every { seenTenderRepository.saveAll(any<List<SeenTender>>()) } returns emptyList()
        every { composer.compose(any(), any()) } returns mockk(relaxed = true)
    }

    // ── runCycle ──────────────────────────────────────────────────────────────

    @Test
    fun `runCycle processes all active subscriptions`() {
        val subs = listOf(buildSubscription(1L), buildSubscription(2L))
        every { subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE) } returns subs
        every { tenderSource.fetch(any(), any()) } returns emptyList()
        every { seenTenderRepository.findPurchaseNumbersBySubscriptionId(any()) } returns emptyList()

        service.runCycle()

        verify(exactly = 2) { tenderSource.fetch(any(), any()) }
    }

    // ── processSubscription — baseline (first run) ────────────────────────────

    @Test
    fun `first run sets baseline and does not send email`() {
        val sub = buildSubscription(lastCheckedAt = null)

        service.processSubscription(sub)

        assertThat(sub.lastCheckedAt).isNotNull()
        verify(exactly = 0) { tenderSource.fetch(any(), any()) }
        verify(exactly = 0) { emailService.send(any()) }
    }

    // ── processSubscription — new tenders ─────────────────────────────────────

    @Test
    fun `new tenders are saved and email is sent`() {
        val sub = buildSubscription(lastCheckedAt = Instant.now().minusSeconds(3600))
        val tenders = listOf(buildTender("NUM-001"), buildTender("NUM-002"))

        every { tenderSource.fetch(any(), any()) } returns tenders
        every { seenTenderRepository.findPurchaseNumbersBySubscriptionId(sub.id) } returns emptyList()

        service.processSubscription(sub)

        verify(exactly = 1) { seenTenderRepository.saveAll(match<List<SeenTender>> { it.size == 2 }) }
        verify(exactly = 1) { emailService.send(any()) }
        assertThat(sub.lastCheckedAt).isAfter(Instant.now().minusSeconds(5))
    }

    // ── processSubscription — all seen ────────────────────────────────────────

    @Test
    fun `already seen tenders do not trigger email`() {
        val sub = buildSubscription(lastCheckedAt = Instant.now().minusSeconds(3600))
        val tenders = listOf(buildTender("NUM-001"), buildTender("NUM-002"))

        every { tenderSource.fetch(any(), any()) } returns tenders
        every { seenTenderRepository.findPurchaseNumbersBySubscriptionId(sub.id) } returns
            listOf("NUM-001", "NUM-002")

        service.processSubscription(sub)

        verify(exactly = 0) { seenTenderRepository.saveAll(any<List<SeenTender>>()) }
        verify(exactly = 0) { emailService.send(any()) }
    }

    @Test
    fun `only new tenders (not seen ones) are saved and emailed`() {
        val sub = buildSubscription(lastCheckedAt = Instant.now().minusSeconds(3600))
        val tenders = listOf(buildTender("NUM-001"), buildTender("NUM-002"), buildTender("NUM-003"))

        every { tenderSource.fetch(any(), any()) } returns tenders
        every { seenTenderRepository.findPurchaseNumbersBySubscriptionId(sub.id) } returns listOf("NUM-001")

        service.processSubscription(sub)

        verify(exactly = 1) { seenTenderRepository.saveAll(match<List<SeenTender>> { it.size == 2 }) }
        verify(exactly = 1) { emailService.send(any()) }
    }

    // ── processSubscription — source failure ──────────────────────────────────

    @Test
    fun `source fetch failure does not update lastCheckedAt and subscription stays active`() {
        val checkedAt = Instant.now().minusSeconds(3600)
        val sub = buildSubscription(lastCheckedAt = checkedAt)

        every { tenderSource.fetch(any(), any()) } throws RuntimeException("API error")

        service.processSubscription(sub)

        assertThat(sub.lastCheckedAt).isEqualTo(checkedAt)
        assertThat(sub.status).isEqualTo(SubscriptionStatus.ACTIVE)
        verify(exactly = 0) { emailService.send(any()) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildSubscription(
        id: Long = 1L,
        source: String = "GOSPLAN_44",
        lastCheckedAt: Instant? = Instant.now().minusSeconds(1800),
    ) = Subscription(
        id = id,
        source = source,
        emails = "[\"test@example.com\"]",
        status = SubscriptionStatus.ACTIVE,
        lastCheckedAt = lastCheckedAt,
    )

    private fun buildTender(purchaseNumber: String) = Tender(
        purchaseNumber = purchaseNumber,
        objectInfo = "Test tender",
        customerInn = "7710538450",
        maxPrice = BigDecimal("100000"),
        currency = "RUB",
        deadline = Instant.now().plusSeconds(86400),
        publishedAt = Instant.now(),
        eisUrl = "https://zakupki.gov.ru/test/$purchaseNumber",
        source = "GOSPLAN_44",
    )
}
