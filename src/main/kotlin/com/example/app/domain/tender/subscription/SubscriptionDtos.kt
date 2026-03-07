package com.example.app.domain.tender.subscription

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.Instant

data class SubscriptionFiltersRequest(
    val regions: List<Int> = emptyList(),
    val keywords: List<String> = emptyList(),
    val customerInn: String? = null,
    val maxPriceFrom: BigDecimal? = null,
    val maxPriceTo: BigDecimal? = null,
)

data class SubscriptionRequest(
    @field:NotBlank val source: String,
    val label: String? = null,
    @field:NotEmpty val emails: List<@Email String>,
    @field:Valid val filters: SubscriptionFiltersRequest = SubscriptionFiltersRequest(),
)

data class SubscriptionUpdateRequest(
    val label: String? = null,
    @field:NotEmpty val emails: List<@Email String>,
    @field:Valid val filters: SubscriptionFiltersRequest = SubscriptionFiltersRequest(),
)

data class SubscriptionStatusRequest(
    val status: SubscriptionStatus,
)

data class SubscriptionResponse(
    val id: Long,
    val source: String,
    val label: String?,
    val emails: List<String>,
    val status: String,
    val filters: SubscriptionFiltersRequest,
    val lastCheckedAt: Instant?,
    val createdAt: Instant,
)

private val dtoMapper = jacksonObjectMapper()

fun Subscription.toResponse() =
    SubscriptionResponse(
        id = id,
        source = source,
        label = label,
        emails = emailList(),
        status = status.name,
        filters =
            SubscriptionFiltersRequest(
                regions = filterRegions?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList(),
                keywords = filterObjectInfo?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                customerInn = filterCustomerInn,
                maxPriceFrom = filterMaxPriceFrom,
                maxPriceTo = filterMaxPriceTo,
            ),
        lastCheckedAt = lastCheckedAt,
        createdAt = createdAt,
    )

fun SubscriptionRequest.toEntity(): Subscription =
    Subscription(
        source = source,
        label = label,
        emails = dtoMapper.writeValueAsString(emails),
        filterRegions = filters.regions.takeIf { it.isNotEmpty() }?.joinToString(","),
        filterObjectInfo = filters.keywords.takeIf { it.isNotEmpty() }?.joinToString(","),
        filterCustomerInn = filters.customerInn,
        filterMaxPriceFrom = filters.maxPriceFrom,
        filterMaxPriceTo = filters.maxPriceTo,
    )
