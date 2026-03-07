package com.example.app.domain.tender.source

import java.math.BigDecimal

data class TenderFilters(
    val regions: List<Int> = emptyList(),
    val keywords: List<String> = emptyList(),
    val customerInn: String? = null,
    val maxPriceFrom: BigDecimal? = null,
    val maxPriceTo: BigDecimal? = null,
)
