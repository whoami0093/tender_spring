package com.example.app.domain.tender.source.gosplan

import com.example.app.domain.tender.source.Tender
import com.example.app.domain.tender.source.TenderFilters
import com.example.app.domain.tender.source.TenderSource
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant

@Component
class GosplanFz223Source(private val gosplanRestClient: RestClient) : TenderSource {

    override val sourceKey = "GOSPLAN_223"

    override fun fetch(filters: TenderFilters, publishedAfter: Instant): List<Tender> {
        val response = gosplanRestClient.get()
            .uri { builder ->
                builder.path("/fz223/purchases")
                    .queryParam("published_after", publishedAfter.toString())
                    .queryParam("limit", 100)
                    .apply {
                        filters.objectInfo?.let { queryParam("object_info", it) }
                        filters.customerInn?.let { queryParam("customer", it) }
                        filters.maxPriceFrom?.let { queryParam("max_price_ge", it) }
                        filters.maxPriceTo?.let { queryParam("max_price_le", it) }
                        filters.regions.forEach { queryParam("region", it) }
                    }
                    .build()
            }
            .retrieve()
            .body(object : ParameterizedTypeReference<List<GosplanPurchaseDto>>() {})
            ?: emptyList()

        return response.map { it.toTender(sourceKey) }
    }
}
