package com.example.app.domain.tender

import com.example.app.common.exception.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.Instant

class TenderServiceTest {
    private val repository: TenderRepository = mockk()
    private lateinit var service: TenderService

    @BeforeEach
    fun setUp() {
        service = TenderService(repository)
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    fun `findAll returns page response with mapped tenders`() {
        val tenders = listOf(buildTender(1L), buildTender(2L))
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        every { repository.findAll(any<Specification<Tender>>(), pageable) } returns PageImpl(tenders, pageable, 2)

        val result = service.findAll(TenderFilter(), pageable)

        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content).hasSize(2)
        assertThat(result.content.map { it.id }).containsExactly(1L, 2L)
        assertThat(result.page).isEqualTo(0)
        assertThat(result.size).isEqualTo(20)
    }

    @Test
    fun `findAll with takenInWork filter passes it to specification`() {
        val pageable = PageRequest.of(0, 20)
        every { repository.findAll(any<Specification<Tender>>(), pageable) } returns PageImpl(emptyList(), pageable, 0)

        service.findAll(TenderFilter(takenInWork = true), pageable)

        verify(exactly = 1) { repository.findAll(any<Specification<Tender>>(), pageable) }
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    fun `findById returns response when tender exists`() {
        every { repository.findByIdOrNull(1L) } returns buildTender(1L)

        val result = service.findById(1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.purchaseNumber).isEqualTo("NUM-1")
    }

    @Test
    fun `findById throws NotFoundException when tender not found`() {
        every { repository.findByIdOrNull(99L) } returns null

        assertThatThrownBy { service.findById(99L) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("99")
    }

    // ── patch ────────────────────────────────────────────────────────────────

    @Test
    fun `patch updates takenInWork and returns updated tender`() {
        val tender = buildTender(1L, takenInWork = false)
        val savedSlot = slot<Tender>()
        every { repository.findByIdOrNull(1L) } returns tender
        every { repository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.patch(1L, PatchTenderRequest(takenInWork = true))

        assertThat(result.takenInWork).isTrue()
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `patch throws NotFoundException when tender not found`() {
        every { repository.findByIdOrNull(99L) } returns null

        assertThatThrownBy { service.patch(99L, PatchTenderRequest(takenInWork = true)) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("99")
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun buildTender(
        id: Long,
        takenInWork: Boolean = false,
    ) = Tender(
        id = id,
        purchaseNumber = "NUM-$id",
        title = "Tender $id",
        region = "Москва",
        customer = "ООО Тест",
        customerInn = "7701234567",
        amount = BigDecimal("100000.00"),
        currency = "RUB",
        status = TenderStatus.SENT,
        deadline = Instant.now().plusSeconds(86400),
        publishedAt = Instant.now(),
        eisUrl = "https://zakupki.gov.ru/$id",
        takenInWork = takenInWork,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}
