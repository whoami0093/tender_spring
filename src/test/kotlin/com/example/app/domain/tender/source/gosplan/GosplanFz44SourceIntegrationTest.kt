package com.example.app.domain.tender.source.gosplan

import com.example.app.domain.tender.source.TenderFilters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

@Disabled("Manual only — makes real HTTP request to v2test.gosplan.info")
class GosplanFz44SourceIntegrationTest {
    private val log = LoggerFactory.getLogger(javaClass)

    private val props =
        GosplanProperties(
            baseUrl = "https://v2test.gosplan.info",
            apiKey = null,
            connTimeoutSeconds = 15,
        )
    private val restClient = GosplanClientConfig().gosplanRestClient(props)
    private val source = GosplanFz44Source(restClient)

    @Test
    fun `fetch returns tenders for keyword and region`() {
        val filters =
            TenderFilters(
                keywords = listOf("хоз товары"),
                regions = listOf(55),
            )
        val publishedAfter = Instant.now().minus(30, ChronoUnit.DAYS)

        val tenders = source.fetch(filters, publishedAfter)

        log.info("Fetched {} tenders", tenders.size)
        tenders.take(3).forEach { log.info("  {} | {} | {}", it.purchaseNumber, it.maxPrice, it.objectInfo) }

        assertThat(tenders).isNotEmpty
        assertThat(tenders).allSatisfy { tender ->
            assertThat(tender.purchaseNumber).isNotBlank()
            assertThat(tender.source).isEqualTo("GOSPLAN_44")
        }
    }

    @Test
    fun `fetch without filters returns tenders`() {
        val publishedAfter = Instant.now().minus(7, ChronoUnit.DAYS)

        val tenders = source.fetch(TenderFilters(), publishedAfter)

        log.info("Fetched {} tenders", tenders.size)
        assertThat(tenders).isNotEmpty
    }
}
