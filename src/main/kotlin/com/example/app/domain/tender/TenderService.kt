package com.example.app.domain.tender

import com.example.app.common.exception.NotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class TenderService(
    private val tenderRepository: TenderRepository,
) {
    fun findAll(
        filter: TenderFilter,
        pageable: Pageable,
    ): TenderPageResponse {
        val spec = TenderSpecification.build(filter)
        val page = tenderRepository.findAll(spec, pageable)
        return TenderPageResponse(
            content = page.content.map { it.toResponse() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            size = page.size,
        )
    }

    fun findById(id: Long): TenderResponse =
        tenderRepository.findByIdOrNull(id)?.toResponse()
            ?: throw NotFoundException("Tender with id=$id not found")

    @Transactional
    fun patch(
        id: Long,
        request: PatchTenderRequest,
    ): TenderResponse {
        val tender =
            tenderRepository.findByIdOrNull(id)
                ?: throw NotFoundException("Tender with id=$id not found")
        tender.takenInWork = request.takenInWork
        tender.updatedAt = Instant.now()
        return tenderRepository.save(tender).toResponse()
    }
}
