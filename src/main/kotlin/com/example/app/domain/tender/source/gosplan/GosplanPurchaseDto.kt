package com.example.app.domain.tender.source.gosplan

import com.example.app.domain.tender.source.Tender
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

data class GosplanPurchaseDto(
    @JsonProperty("purchase_number") val purchaseNumber: String,
    @JsonProperty("object_info") val objectInfo: String?,
    @JsonProperty("customers") val customers: List<String>?,
    @JsonProperty("max_price") val maxPrice: BigDecimal?,
    @JsonProperty("currency_code") val currencyCode: String?,
    @JsonProperty("collecting_finished_at") val collectingFinishedAt: LocalDateTime?,
    @JsonProperty("submission_close_at") val submissionCloseAt: LocalDateTime?,
    @JsonProperty("published_at") val publishedAt: LocalDateTime?,
    @JsonProperty("region") val region: Int?,
)

fun GosplanPurchaseDto.toTender(source: String): Tender {
    val deadline = (collectingFinishedAt ?: submissionCloseAt)?.toInstant(ZoneOffset.UTC)
    val eisUrl =
        if (source == "GOSPLAN_44") {
            "https://zakupki.gov.ru/epz/order/notice/ea44/view/common-info.html?regNumber=$purchaseNumber"
        } else {
            "https://zakupki.gov.ru/223/purchase/public/purchase/view/info.html?regNumber=$purchaseNumber"
        }
    return Tender(
        purchaseNumber = purchaseNumber,
        objectInfo = objectInfo ?: "",
        customerInn = customers?.firstOrNull(),
        maxPrice = maxPrice,
        currency = currencyCode ?: "RUB",
        deadline = deadline,
        publishedAt = publishedAt?.toInstant(ZoneOffset.UTC),
        eisUrl = eisUrl,
        source = source,
    )
}
