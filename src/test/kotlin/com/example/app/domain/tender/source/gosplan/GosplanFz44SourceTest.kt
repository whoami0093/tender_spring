package com.example.app.domain.tender.source.gosplan

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

class GosplanFz44SourceTest {
    private val source = GosplanFz44Source(mockk(relaxed = true))

    @Test
    fun `sourceKey is GOSPLAN_44`() {
        assertThat(source.sourceKey).isEqualTo("GOSPLAN_44")
    }

    // ── toTender mapping ─────────────────────────────────────────────────────

    @Test
    fun `toTender maps all fields correctly for GOSPLAN_44`() {
        val dto =
            GosplanPurchaseDto(
                purchaseNumber = "0123456789012345678",
                purchaseType = "purchaseEa44",
                objectInfo = "Поставка компьютеров",
                customers = listOf("7710538450"),
                maxPrice = BigDecimal("500000.00"),
                currencyCode = "RUB",
                collectingFinishedAt = LocalDateTime.of(2026, 4, 1, 10, 0, 0),
                submissionCloseAt = null,
                publishedAt = LocalDateTime.of(2026, 3, 1, 8, 0, 0),
                region = 77,
            )

        val tender = dto.toTender("GOSPLAN_44")

        assertThat(tender.purchaseNumber).isEqualTo("0123456789012345678")
        assertThat(tender.objectInfo).isEqualTo("Поставка компьютеров")
        assertThat(tender.customerInn).isEqualTo("7710538450")
        assertThat(tender.maxPrice).isEqualByComparingTo(BigDecimal("500000.00"))
        assertThat(tender.currency).isEqualTo("RUB")
        assertThat(tender.deadline).isEqualTo(Instant.parse("2026-04-01T10:00:00Z"))
        assertThat(tender.publishedAt).isEqualTo(Instant.parse("2026-03-01T08:00:00Z"))
        assertThat(tender.source).isEqualTo("GOSPLAN_44")
        assertThat(tender.eisUrl).contains("0123456789012345678")
        assertThat(tender.eisUrl).contains("ea44")
    }

    @Test
    fun `toTender handles null optional fields`() {
        val dto =
            GosplanPurchaseDto(
                purchaseNumber = "NUM-001",
                purchaseType = null,
                objectInfo = null,
                customers = null,
                maxPrice = null,
                currencyCode = null,
                collectingFinishedAt = null,
                submissionCloseAt = null,
                publishedAt = null,
                region = null,
            )

        val tender = dto.toTender("GOSPLAN_44")

        assertThat(tender.objectInfo).isEmpty()
        assertThat(tender.customerInn).isNull()
        assertThat(tender.maxPrice).isNull()
        assertThat(tender.currency).isEqualTo("RUB")
        assertThat(tender.deadline).isNull()
    }

    @Test
    fun `toTender picks first customer INN when multiple customers provided`() {
        val dto =
            GosplanPurchaseDto(
                purchaseNumber = "NUM-001",
                purchaseType = null,
                objectInfo = "Test",
                customers = listOf("1111111111", "2222222222"),
                maxPrice = null,
                currencyCode = null,
                collectingFinishedAt = null,
                submissionCloseAt = null,
                publishedAt = null,
                region = null,
            )

        assertThat(dto.toTender("GOSPLAN_44").customerInn).isEqualTo("1111111111")
    }

    // ── 223-FZ URL mapping ───────────────────────────────────────────────────

    @Test
    fun `toTender builds correct EIS URL for 223-FZ with purchaseNoticeZKESMBO`() {
        val dto =
            GosplanPurchaseDto(
                purchaseNumber = "32615886135",
                purchaseType = "purchaseNoticeZKESMBO",
                objectInfo = "Поставка реагентов",
                customers = null,
                maxPrice = null,
                currencyCode = null,
                collectingFinishedAt = null,
                submissionCloseAt = null,
                publishedAt = null,
                region = null,
            )

        val tender = dto.toTender("GOSPLAN_223")

        assertThat(tender.eisUrl).contains("noticeZKESMBO")
        assertThat(tender.eisUrl).contains("32615886135")
        assertThat(tender.eisUrl).doesNotContain("223/purchase/public")
    }

    @Test
    fun `toTender falls back to notice223 when purchaseType is null for 223-FZ`() {
        val dto =
            GosplanPurchaseDto(
                purchaseNumber = "32615886135",
                purchaseType = null,
                objectInfo = "Test",
                customers = null,
                maxPrice = null,
                currencyCode = null,
                collectingFinishedAt = null,
                submissionCloseAt = null,
                publishedAt = null,
                region = null,
            )

        val tender = dto.toTender("GOSPLAN_223")

        assertThat(tender.eisUrl).contains("notice223")
        assertThat(tender.eisUrl).contains("32615886135")
    }
}
