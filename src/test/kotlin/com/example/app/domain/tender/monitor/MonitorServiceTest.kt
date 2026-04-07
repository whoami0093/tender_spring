package com.example.app.domain.tender.monitor

import com.example.app.common.email.EmailService
import com.example.app.domain.tender.TenderRepository
import com.example.app.domain.tender.Tender as TenderEntity
import com.example.app.domain.tender.source.Tender
import com.example.app.domain.tender.source.TenderSource
import com.example.app.domain.tender.source.TenderSourceRegistry
import com.example.app.domain.tender.subscription.Subscription
import com.example.app.domain.tender.subscription.SubscriptionRepository
import com.example.app.domain.tender.subscription.SubscriptionStatus
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class MonitorServiceTest {
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val subscriptionProcessor = mockk<SubscriptionProcessor>(relaxed = true)

    private val service = MonitorService(subscriptionRepository, subscriptionProcessor, betweenSubscriptionsMs = 0)

    @Test
    fun `runCycle delegates to processor for each active subscription`() {
        every { subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE) } returns
            listOf(buildSubscription(1L), buildSubscription(2L))

        service.runCycle()

        verify(exactly = 1) { subscriptionProcessor.process(1L) }
        verify(exactly = 1) { subscriptionProcessor.process(2L) }
    }

    @Test
    fun `runCycle does nothing when no active subscriptions`() {
        every { subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE) } returns emptyList()

        service.runCycle()

        verify(exactly = 0) { subscriptionProcessor.process(any()) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildSubscription(id: Long) =
        Subscription(
            id = id,
            source = "GOSPLAN_44",
            emails = "[\"test@example.com\"]",
            status = SubscriptionStatus.ACTIVE,
        )
}

class SubscriptionProcessorTest {
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val tenderRepository = mockk<TenderRepository>()
    private val seenTenderRepository = mockk<SeenTenderRepository>()
    private val registry = mockk<TenderSourceRegistry>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val composer = mockk<TenderEmailComposer>()
    private val meterRegistry = mockk<MeterRegistry>(relaxed = true)

    private val processor =
        SubscriptionProcessor(
            subscriptionRepository,
            seenTenderRepository,
            tenderRepository,
            registry,
            emailService,
            composer,
            meterRegistry,
        )

    private val tenderSource = mockk<TenderSource>()

    @BeforeEach
    fun setUp() {
        every { registry.get(any()) } returns tenderSource
        every { seenTenderRepository.saveAll(any<List<SeenTender>>()) } returns emptyList()
        every { tenderRepository.findExistingPurchaseNumbers(any()) } returns emptyList()
        every { tenderRepository.saveAll(any<Iterable<TenderEntity>>()) } returns emptyList()
        every { composer.compose(any(), any()) } returns mockk(relaxed = true)
    }

    // ── first run (baseline) ──────────────────────────────────────────────────

    @Test
    fun `first run sets baseline and does not send email`() {
        val sub = buildSubscription(lastCheckedAt = null)
        every { subscriptionRepository.findById(sub.id) } returns Optional.of(sub)

        processor.process(sub.id)

        assertThat(sub.lastCheckedAt).isNotNull()
        verify(exactly = 0) { tenderSource.fetch(any(), any()) }
        verify(exactly = 0) { emailService.send(any()) }
    }

    // ── new tenders ────────────────────────────────────────────────────────────

    @Test
    fun `new tenders are saved and email is sent`() {
        val sub = buildSubscription(lastCheckedAt = Instant.now().minusSeconds(3600))
        val tenders = listOf(buildTender("NUM-001"), buildTender("NUM-002"))

        every { subscriptionRepository.findById(sub.id) } returns Optional.of(sub)
        every { tenderSource.fetch(any(), any()) } returns tenders
        every { seenTenderRepository.findSeenNumbers(sub.id, any()) } returns emptyList()

        processor.process(sub.id)

        verify(exactly = 1) { seenTenderRepository.saveAll(match<List<SeenTender>> { it.size == 2 }) }
        verify(exactly = 1) { tenderRepository.saveAll(match<Iterable<TenderEntity>> { it.toList().size == 2 }) }
        verify(exactly = 1) { emailService.send(any()) }
        assertThat(sub.lastCheckedAt).isAfter(Instant.now().minusSeconds(5))
    }

    // ── all seen ───────────────────────────────────────────────────────────────

    @Test
    fun `already seen tenders do not trigger email`() {
        val sub = buildSubscription(lastCheckedAt = Instant.now().minusSeconds(3600))
        val tenders = listOf(buildTender("NUM-001"), buildTender("NUM-002"))

        every { subscriptionRepository.findById(sub.id) } returns Optional.of(sub)
        every { tenderSource.fetch(any(), any()) } returns tenders
        every { seenTenderRepository.findSeenNumbers(sub.id, any()) } returns listOf("NUM-001", "NUM-002")

        processor.process(sub.id)

        verify(exactly = 0) { seenTenderRepository.saveAll(any<List<SeenTender>>()) }
        verify(exactly = 0) { emailService.send(any()) }
    }

    // ── partial new ────────────────────────────────────────────────────────────

    @Test
    fun `only new tenders (not seen) are saved and emailed`() {
        val sub = buildSubscription(lastCheckedAt = Instant.now().minusSeconds(3600))
        val tenders = listOf(buildTender("NUM-001"), buildTender("NUM-002"), buildTender("NUM-003"))

        every { subscriptionRepository.findById(sub.id) } returns Optional.of(sub)
        every { tenderSource.fetch(any(), any()) } returns tenders
        every { seenTenderRepository.findSeenNumbers(sub.id, any()) } returns listOf("NUM-001")

        processor.process(sub.id)

        verify(exactly = 1) { seenTenderRepository.saveAll(match<List<SeenTender>> { it.size == 2 }) }
        verify(exactly = 1) { emailService.send(any()) }
    }

    // ── API failure ────────────────────────────────────────────────────────────

    @Test
    fun `source fetch failure does not update lastCheckedAt and subscription stays active`() {
        val checkedAt = Instant.now().minusSeconds(3600)
        val sub = buildSubscription(lastCheckedAt = checkedAt)

        every { subscriptionRepository.findById(sub.id) } returns Optional.of(sub)
        every { tenderSource.fetch(any(), any()) } throws RuntimeException("API error")

        processor.process(sub.id)

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

    private fun buildTender(purchaseNumber: String) =
        Tender(
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
