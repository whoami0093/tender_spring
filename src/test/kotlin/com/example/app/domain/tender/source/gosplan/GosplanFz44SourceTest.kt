package com.example.app.domain.tender.source.gosplan

import com.example.app.domain.tender.source.TenderFilters
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Instant

class GosplanFz44SourceTest {

    private val source = GosplanFz44Source(mockk(relaxed = true))

    @Test
    fun `sourceKey is GOSPLAN_44`() {
        assertThat(source.sourceKey).isEqualTo("GOSPLAN_44")
    }

    @Test
    fun `toTender maps all fields correctly for GOSPLAN_44`() {
        val dto = GosplanPurchaseDto(
            purchaseNumber = "0123456789012345678",
            objectInfo = "Поставка компьютеров",
            customers = listOf("7710538450"),
            maxPrice = BigDecimal("500000.00"),
            currencyCode = "RUB",
            collectingFinishedAt = Instant.parse("2026-04-01T10:00:00Z"),
            submissionCloseAt = null,
            publishedAt = Instant.parse("2026-03-01T08:00:00Z"),
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
        val dto = GosplanPurchaseDto(
            purchaseNumber = "NUM-001",
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
    fun `TenderFilters defaults are empty`() {
        val filters = TenderFilters()
        assertThat(filters.regions).isEmpty()
        assertThat(filters.objectInfo).isNull()
        assertThat(filters.customerInn).isNull()
        assertThat(filters.maxPriceFrom).isNull()
        assertThat(filters.maxPriceTo).isNull()
    }
}
