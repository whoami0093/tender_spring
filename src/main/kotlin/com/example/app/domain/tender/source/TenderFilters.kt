package com.example.app.domain.tender.source

import java.math.BigDecimal

data class TenderFilters(
    val regions: List<Int> = emptyList(),
    val keywords: List<String> = emptyList(),
    val localKeywords: List<String> = emptyList(),
    val customerInn: String? = null,
    val maxPriceFrom: BigDecimal? = null,
    val maxPriceTo: BigDecimal? = null,
) {
    /**
     * Returns true when the tender passes the local keyword filter.
     * If [localKeywords] is empty, every tender passes.
     * Otherwise at least one local keyword must appear (case-insensitive substring) in [Tender.objectInfo].
     */
    fun matchesTender(tender: Tender): Boolean {
        if (localKeywords.isEmpty()) return true
        val info = tender.objectInfo.lowercase()
        return localKeywords.any { keyword -> keyword.lowercase() in info }
    }
}
