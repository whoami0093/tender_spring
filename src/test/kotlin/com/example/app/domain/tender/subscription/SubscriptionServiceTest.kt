package com.example.app.domain.tender.subscription

import com.example.app.common.exception.NotFoundException
import com.example.app.domain.tender.source.TenderSource
import com.example.app.domain.tender.source.TenderSourceRegistry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.Instant

class SubscriptionServiceTest {
    private val repository: SubscriptionRepository = mockk()
    private val registry: TenderSourceRegistry = mockk()
    private lateinit var service: SubscriptionService

    @BeforeEach
    fun setUp() {
        service = SubscriptionService(repository, registry)
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    fun `findAll returns mapped responses`() {
        every { repository.findAll() } returns listOf(buildSubscription(1L), buildSubscription(2L))

        val result = service.findAll()

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly(1L, 2L)
        assertThat(result.map { it.source }).containsOnly("GOSPLAN_44")
    }

    @Test
    fun `findAll returns empty list when no subscriptions exist`() {
        every { repository.findAll() } returns emptyList()

        assertThat(service.findAll()).isEmpty()
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    fun `findById returns mapped response when found`() {
        every { repository.findByIdOrNull(1L) } returns buildSubscription(1L)

        val result = service.findById(1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.source).isEqualTo("GOSPLAN_44")
        assertThat(result.status).isEqualTo("ACTIVE")
        assertThat(result.emails).containsExactly("user@example.com")
    }

    @Test
    fun `findById throws NotFoundException when subscription not found`() {
        every { repository.findByIdOrNull(99L) } returns null

        assertThatThrownBy { service.findById(99L) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("99")
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    fun `create validates source via registry and saves entity`() {
        val req = SubscriptionRequest(source = "GOSPLAN_44", emails = listOf("user@example.com"))
        every { registry.get("GOSPLAN_44") } returns mockk<TenderSource>()
        val slot = slot<Subscription>()
        every { repository.save(capture(slot)) } returns buildSubscription(10L)

        val result = service.create(req)

        assertThat(result.id).isEqualTo(10L)
        assertThat(slot.captured.source).isEqualTo("GOSPLAN_44")
        verify(exactly = 1) { registry.get("GOSPLAN_44") }
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `create throws NotFoundException for unknown source and does not save`() {
        every { registry.get("UNKNOWN") } throws NotFoundException("Unknown tender source: UNKNOWN")

        assertThatThrownBy {
            service.create(SubscriptionRequest(source = "UNKNOWN", emails = listOf("u@test.com")))
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("UNKNOWN")

        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `create persists all filter fields correctly`() {
        val req =
            SubscriptionRequest(
                source = "GOSPLAN_44",
                label = "My Sub",
                emails = listOf("a@test.com", "b@test.com"),
                filters =
                    SubscriptionFiltersRequest(
                        regions = listOf(77, 50),
                        keywords = listOf("строительство", "ремонт"),
                        localKeywords = listOf("хоз"),
                        customerInn = "7710538450",
                        maxPriceFrom = BigDecimal("1000"),
                        maxPriceTo = BigDecimal("5000000"),
                    ),
            )
        every { registry.get("GOSPLAN_44") } returns mockk<TenderSource>()
        val slot = slot<Subscription>()
        every { repository.save(capture(slot)) } returns buildSubscription(1L)

        service.create(req)

        with(slot.captured) {
            assertThat(label).isEqualTo("My Sub")
            assertThat(filterRegions).isEqualTo("77,50")
            assertThat(filterObjectInfo).isEqualTo("строительство,ремонт")
            assertThat(filterLocalKeywords).isEqualTo("хоз")
            assertThat(filterCustomerInn).isEqualTo("7710538450")
            assertThat(filterMaxPriceFrom).isEqualByComparingTo(BigDecimal("1000"))
            assertThat(filterMaxPriceTo).isEqualByComparingTo(BigDecimal("5000000"))
        }
    }

    @Test
    fun `create stores null for empty filter lists`() {
        val req =
            SubscriptionRequest(
                source = "GOSPLAN_44",
                emails = listOf("u@test.com"),
                filters = SubscriptionFiltersRequest(), // all empty defaults
            )
        every { registry.get("GOSPLAN_44") } returns mockk<TenderSource>()
        val slot = slot<Subscription>()
        every { repository.save(capture(slot)) } returns buildSubscription(1L)

        service.create(req)

        assertThat(slot.captured.filterRegions).isNull()
        assertThat(slot.captured.filterObjectInfo).isNull()
        assertThat(slot.captured.filterLocalKeywords).isNull()
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    fun `update modifies all entity fields in place and returns response`() {
        val entity = buildSubscription(1L)
        every { repository.findByIdOrNull(1L) } returns entity

        val req =
            SubscriptionUpdateRequest(
                label = "Updated Label",
                emails = listOf("new@test.com"),
                filters =
                    SubscriptionFiltersRequest(
                        regions = listOf(77),
                        keywords = listOf("ремонт"),
                        localKeywords = listOf("хоз", "уборк"),
                        customerInn = "7710538450",
                        maxPriceFrom = BigDecimal("500"),
                        maxPriceTo = BigDecimal("100000"),
                    ),
            )

        val result = service.update(1L, req)

        assertThat(entity.label).isEqualTo("Updated Label")
        assertThat(entity.filterRegions).isEqualTo("77")
        assertThat(entity.filterObjectInfo).isEqualTo("ремонт")
        assertThat(entity.filterLocalKeywords).isEqualTo("хоз,уборк")
        assertThat(entity.filterCustomerInn).isEqualTo("7710538450")
        assertThat(entity.filterMaxPriceFrom).isEqualByComparingTo(BigDecimal("500"))
        assertThat(entity.filterMaxPriceTo).isEqualByComparingTo(BigDecimal("100000"))
        assertThat(entity.updatedAt).isAfter(Instant.now().minusSeconds(5))
        assertThat(result.emails).containsExactly("new@test.com")
    }

    @Test
    fun `update clears filter fields when empty lists are provided`() {
        val entity =
            buildSubscription(
                1L,
                filterRegions = "77",
                filterObjectInfo = "строительство",
                filterLocalKeywords = "хоз",
            )
        every { repository.findByIdOrNull(1L) } returns entity

        service.update(1L, SubscriptionUpdateRequest(emails = listOf("u@test.com")))

        assertThat(entity.filterRegions).isNull()
        assertThat(entity.filterObjectInfo).isNull()
        assertThat(entity.filterLocalKeywords).isNull()
    }

    @Test
    fun `update throws NotFoundException when subscription not found`() {
        every { repository.findByIdOrNull(99L) } returns null

        assertThatThrownBy {
            service.update(99L, SubscriptionUpdateRequest(emails = listOf("u@test.com")))
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("99")
    }

    // ── updateStatus ─────────────────────────────────────────────────────────

    @Test
    fun `updateStatus changes status to PAUSED and returns updated response`() {
        val entity = buildSubscription(1L)
        every { repository.findByIdOrNull(1L) } returns entity

        val result = service.updateStatus(1L, SubscriptionStatus.PAUSED)

        assertThat(entity.status).isEqualTo(SubscriptionStatus.PAUSED)
        assertThat(result.status).isEqualTo("PAUSED")
        assertThat(entity.updatedAt).isAfter(Instant.now().minusSeconds(5))
    }

    @Test
    fun `updateStatus throws NotFoundException when subscription not found`() {
        every { repository.findByIdOrNull(99L) } returns null

        assertThatThrownBy { service.updateStatus(99L, SubscriptionStatus.PAUSED) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("99")
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    fun `delete removes subscription when it exists`() {
        every { repository.existsById(1L) } returns true
        every { repository.deleteById(1L) } just runs

        service.delete(1L)

        verify(exactly = 1) { repository.deleteById(1L) }
    }

    @Test
    fun `delete throws NotFoundException and skips deleteById when not found`() {
        every { repository.existsById(99L) } returns false

        assertThatThrownBy { service.delete(99L) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("99")

        verify(exactly = 0) { repository.deleteById(any()) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildSubscription(
        id: Long = 1L,
        source: String = "GOSPLAN_44",
        filterRegions: String? = null,
        filterObjectInfo: String? = null,
        filterLocalKeywords: String? = null,
    ) = Subscription(
        id = id,
        source = source,
        emails = """["user@example.com"]""",
        status = SubscriptionStatus.ACTIVE,
        filterRegions = filterRegions,
        filterObjectInfo = filterObjectInfo,
        filterLocalKeywords = filterLocalKeywords,
    )
}
