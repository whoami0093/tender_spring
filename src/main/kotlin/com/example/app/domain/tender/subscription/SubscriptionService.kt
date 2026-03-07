package com.example.app.domain.tender.subscription

import com.example.app.common.exception.NotFoundException
import com.example.app.domain.tender.source.TenderSourceRegistry
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class SubscriptionService(
    private val repository: SubscriptionRepository,
    private val registry: TenderSourceRegistry,
) {
    private val mapper = jacksonObjectMapper()
    fun findAll(): List<SubscriptionResponse> = repository.findAll().map { it.toResponse() }

    fun findById(id: Long): SubscriptionResponse =
        repository.findByIdOrNull(id)?.toResponse() ?: throw NotFoundException("Subscription not found: $id")

    @Transactional
    fun create(req: SubscriptionRequest): SubscriptionResponse {
        registry.get(req.source)  // validates source exists
        val entity = req.toEntity()
        return repository.save(entity).toResponse()
    }

    @Transactional
    fun update(id: Long, req: SubscriptionUpdateRequest): SubscriptionResponse {
        val entity = repository.findByIdOrNull(id) ?: throw NotFoundException("Subscription not found: $id")
        entity.apply {
            label = req.label
            emails = mapper.writeValueAsString(req.emails)
            filterRegions = req.filters.regions.takeIf { it.isNotEmpty() }?.joinToString(",")
            filterObjectInfo = req.filters.objectInfo
            filterCustomerInn = req.filters.customerInn
            filterMaxPriceFrom = req.filters.maxPriceFrom
            filterMaxPriceTo = req.filters.maxPriceTo
            updatedAt = Instant.now()
        }
        return entity.toResponse()
    }

    @Transactional
    fun updateStatus(id: Long, status: SubscriptionStatus): SubscriptionResponse {
        val entity = repository.findByIdOrNull(id) ?: throw NotFoundException("Subscription not found: $id")
        entity.status = status
        entity.updatedAt = Instant.now()
        return entity.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!repository.existsById(id)) throw NotFoundException("Subscription not found: $id")
        repository.deleteById(id)
    }
}
