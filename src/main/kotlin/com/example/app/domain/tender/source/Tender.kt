package com.example.app.domain.tender.source

import java.math.BigDecimal
import java.time.Instant

data class Tender(
    val purchaseNumber: String,
    val objectInfo: String,
    val customerInn: String?,
    val maxPrice: BigDecimal?,
    val currency: String,
    val deadline: Instant?,
    val publishedAt: Instant?,
    val eisUrl: String,
    val source: String,
)
