package com.example.app.domain.tender

import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object TenderSpecification {
    fun build(filter: TenderFilter): Specification<Tender> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            filter.region?.let {
                predicates += cb.equal(root.get<String>("region"), it)
            }
            filter.customer?.let {
                predicates += cb.like(cb.lower(root.get("customer")), "%${it.lowercase()}%")
            }
            filter.statuses?.takeIf { it.isNotEmpty() }?.let {
                predicates += root.get<TenderStatus>("status").`in`(it)
            }
            filter.takenInWork?.let {
                predicates += cb.equal(root.get<Boolean>("takenInWork"), it)
            }
            filter.deadlineFrom?.let {
                predicates += cb.greaterThanOrEqualTo(root.get("deadline"), it)
            }
            filter.deadlineTo?.let {
                predicates += cb.lessThanOrEqualTo(root.get("deadline"), it)
            }
            filter.amountFrom?.let {
                predicates += cb.ge(root.get("amount"), it)
            }
            filter.amountTo?.let {
                predicates += cb.le(root.get("amount"), it)
            }
            filter.numberSearch?.let {
                predicates += cb.like(cb.lower(root.get("purchaseNumber")), "%${it.lowercase()}%")
            }

            @Suppress("SpreadOperator")
            cb.and(*predicates.toTypedArray())
        }
}
