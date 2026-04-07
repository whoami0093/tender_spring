package com.example.app.domain.tender

import com.example.app.common.exception.BadRequestException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant

@RestController
@RequestMapping("/api/v1/tenders")
class TenderController(
    private val tenderService: TenderService,
) {
    companion object {
        private val ALLOWED_SORT_FIELDS =
            setOf(
                "createdAt",
                "updatedAt",
                "deadline",
                "amount",
                "customer",
                "region",
                "publishedAt",
            )
        private const val MAX_PAGE_SIZE = 100
    }

    @GetMapping
    fun getAll(
        @RequestParam(required = false) region: String?,
        @RequestParam(required = false) customer: String?,
        @RequestParam(required = false) status: List<String>?,
        @RequestParam(required = false) takenInWork: Boolean?,
        @RequestParam(required = false) deadlineFrom: Instant?,
        @RequestParam(required = false) deadlineTo: Instant?,
        @RequestParam(required = false) amountFrom: BigDecimal?,
        @RequestParam(required = false) amountTo: BigDecimal?,
        @RequestParam(required = false) numberSearch: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt,desc") sort: String,
    ): TenderPageResponse {
        val pageable = buildPageable(page, size, sort)
        val statuses = status?.map { TenderStatus.valueOf(it.uppercase()) }
        val filter =
            TenderFilter(
                region = region,
                customer = customer,
                statuses = statuses,
                takenInWork = takenInWork,
                deadlineFrom = deadlineFrom,
                deadlineTo = deadlineTo,
                amountFrom = amountFrom,
                amountTo = amountTo,
                numberSearch = numberSearch,
            )
        return tenderService.findAll(filter, pageable)
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): TenderResponse = tenderService.findById(id)

    @PatchMapping("/{id}")
    fun patch(
        @PathVariable id: Long,
        @RequestBody request: PatchTenderRequest,
    ): TenderResponse = tenderService.patch(id, request)

    private fun buildPageable(
        page: Int,
        size: Int,
        sort: String,
    ): PageRequest {
        val clampedSize = size.coerceAtMost(MAX_PAGE_SIZE)
        val parts = sort.split(",")
        val field = parts[0].trim()
        val direction = if (parts.getOrNull(1)?.trim()?.uppercase() == "ASC") Sort.Direction.ASC else Sort.Direction.DESC
        if (field !in ALLOWED_SORT_FIELDS) {
            throw BadRequestException("Sort field '$field' is not allowed. Allowed: $ALLOWED_SORT_FIELDS")
        }
        return PageRequest.of(page, clampedSize, Sort.by(direction, field))
    }
}
