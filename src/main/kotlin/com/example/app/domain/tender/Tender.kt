package com.example.app.domain.tender

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
@Table(name = "tenders")
class Tender(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true, length = 100)
    val purchaseNumber: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val title: String,
    @Column(length = 255)
    val region: String? = null,
    @Column(length = 500)
    val customer: String? = null,
    @Column(length = 12)
    val customerInn: String? = null,
    @Column(precision = 18, scale = 2)
    val amount: BigDecimal? = null,
    @Column(nullable = false, length = 10)
    val currency: String = "RUB",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val status: TenderStatus = TenderStatus.SENT,
    val deadline: Instant? = null,
    val publishedAt: Instant? = null,
    @Column(columnDefinition = "TEXT")
    val eisUrl: String? = null,
    @Column(nullable = false)
    var takenInWork: Boolean = false,
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
)

enum class TenderStatus { SENT, WON, LOST, IN_PROGRESS }
