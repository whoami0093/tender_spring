package com.example.app.domain.tender.source.gosplan

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

class GosplanFz223SourceTest {
    private val source = GosplanFz223Source(mockk(relaxed = true))

    @Test
    fun `sourceKey is GOSPLAN_223`() {
        assertThat(source.sourceKey).isEqualTo("GOSPLAN_223")
    }

    @Test
    fun `toTender uses submission_close_at as deadline for 223-FZ`() {
        val submissionClose = LocalDateTime.of(2026, 5, 1, 12, 0, 0)
        val dto =
            GosplanPurchaseDto(
                purchaseNumber = "223001002003004005",
                objectInfo = "Закупка оборудования",
                customers = listOf("5010038450"),
                maxPrice = BigDecimal("1200000.00"),
                currencyCode = "RUB",
                collectingFinishedAt = null,
                submissionCloseAt = submissionClose,
                publishedAt = LocalDateTime.of(2026, 3, 10, 9, 0, 0),
                region = 78,
            )

        val tender = dto.toTender("GOSPLAN_223")

        assertThat(tender.deadline).isEqualTo(submissionClose.toInstant(ZoneOffset.UTC))
        assertThat(tender.source).isEqualTo("GOSPLAN_223")
        assertThat(tender.eisUrl).contains("223")
        assertThat(tender.eisUrl).contains("223001002003004005")
    }

    @Test
    fun `toTender prefers collectingFinishedAt over submissionCloseAt when both present`() {
        val collectingFinished = LocalDateTime.of(2026, 4, 15, 10, 0, 0)
        val submissionClose = LocalDateTime.of(2026, 5, 1, 12, 0, 0)
        val dto =
            GosplanPurchaseDto(
                purchaseNumber = "NUM-001",
                objectInfo = "Test",
                customers = null,
                maxPrice = null,
                currencyCode = null,
                collectingFinishedAt = collectingFinished,
                submissionCloseAt = submissionClose,
                publishedAt = null,
                region = null,
            )

        val tender = dto.toTender("GOSPLAN_223")

        assertThat(tender.deadline).isEqualTo(collectingFinished.toInstant(ZoneOffset.UTC))
    }
}
