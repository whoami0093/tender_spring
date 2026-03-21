package com.example.app.domain.tender.subscription

import com.example.app.domain.tender.source.TenderFilters
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = subscriptions)
class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val source: String,
    @Column
    var label: String? = null,
    @Column(nullable = false, columnDefinition = TEXT)
    var emails: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    @Column
    var filterRegions: String? = null,
    @Column
    var filterObjectInfo: String? = null,
    @Column
    var filterCustomerInn: String? = null,
    @Column
    var filterMaxPriceFrom: BigDecimal? = null,
    @Column
    var filterMaxPriceTo: BigDecimal? = null,
    @Column
    var filterLocalKeywords: String? = null,
    @Column
    var lastCheckedAt: Instant? = null,
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
)

enum class SubscriptionStatus { ACTIVE, PAUSED }

fun Subscription.toFilters() =
    TenderFilters(
        regions = filterRegions?.split(,)?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList(),
        keywords = filterObjectInfo?.split(,)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        localKeywords = filterLocalKeywords?.split(,)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        customerInn = filterCustomerInn,
        maxPriceFrom = filterMaxPriceFrom,
        maxPriceTo = filterMaxPriceTo,
    )

private val emailMapper = jacksonObjectMapper()

fun Subscription.emailList(): List<String> = emailMapper.readValue(emails)
