package com.example.app.domain.tender

import java.math.BigDecimal
import java.time.Instant

data class TenderResponse(
    val id: Long,
    val purchaseNumber: String,
    val title: String,
    val region: String?,
    val customer: String?,
    val customerInn: String?,
    val amount: BigDecimal?,
    val currency: String,
    val status: TenderStatus,
    val deadline: Instant?,
    val publishedAt: Instant?,
    val eisUrl: String?,
    val takenInWork: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TenderPageResponse(
    val content: List<TenderResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

data class PatchTenderRequest(
    val takenInWork: Boolean,
)

fun Tender.toResponse() =
    TenderResponse(
        id = id,
        purchaseNumber = purchaseNumber,
        title = title,
        region = region,
        customer = customer,
        customerInn = customerInn,
        amount = amount,
        currency = currency,
        status = status,
        deadline = deadline,
        publishedAt = publishedAt,
        eisUrl = eisUrl,
        takenInWork = takenInWork,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
