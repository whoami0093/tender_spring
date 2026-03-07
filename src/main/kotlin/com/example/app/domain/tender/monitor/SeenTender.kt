package com.example.app.domain.tender.monitor

import com.example.app.domain.tender.source.Tender
import com.example.app.domain.tender.subscription.Subscription
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "seen_tenders")
class SeenTender(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    val subscription: Subscription,

    @Column(nullable = false)
    val purchaseNumber: String,

    @Column
    val objectInfo: String? = null,

    @Column
    val customerInn: String? = null,

    @Column
    val maxPrice: BigDecimal? = null,

    @Column
    val currency: String? = null,

    @Column
    val deadline: Instant? = null,

    @Column
    val publishedAt: Instant? = null,

    @Column(columnDefinition = "TEXT")
    val eisUrl: String? = null,

    @Column(nullable = false)
    val foundAt: Instant = Instant.now(),
) {
    companion object {
        fun from(subscription: Subscription, tender: Tender) = SeenTender(
            subscription = subscription,
            purchaseNumber = tender.purchaseNumber,
            objectInfo = tender.objectInfo,
            customerInn = tender.customerInn,
            maxPrice = tender.maxPrice,
            currency = tender.currency,
            deadline = tender.deadline,
            publishedAt = tender.publishedAt,
            eisUrl = tender.eisUrl,
        )
    }
}
