package com.example.app.domain.tender

import java.math.BigDecimal
import java.time.Instant

data class TenderFilter(
    val region: String? = null,
    val customer: String? = null,
    val statuses: List<TenderStatus>? = null,
    val takenInWork: Boolean? = null,
    val deadlineFrom: Instant? = null,
    val deadlineTo: Instant? = null,
    val amountFrom: BigDecimal? = null,
    val amountTo: BigDecimal? = null,
    val numberSearch: String? = null,
)
