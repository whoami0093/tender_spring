package com.example.app.domain.tender.source

import java.math.BigDecimal

data class TenderFilters(
    val regions: List<Int> = emptyList(),
    val objectInfo: String? = null,
    val customerInn: String? = null,
    val maxPriceFrom: BigDecimal? = null,
    val maxPriceTo: BigDecimal? = null,
)
