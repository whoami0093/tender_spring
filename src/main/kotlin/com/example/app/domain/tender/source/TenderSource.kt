package com.example.app.domain.tender.source

import java.time.Instant

interface TenderSource {
    val sourceKey: String
    fun fetch(filters: TenderFilters, publishedAfter: Instant): List<Tender>
}
